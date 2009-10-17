package org.guicebox.failover;

import com.google.inject.*;
import java.util.*;
import java.util.logging.*;
import net.jcip.annotations.*;
import org.guicebox.*;

/**
 * Implements a simple but effective active-passive failover strategy for high availability. There is a maximum of one
 * primary node in the cluster, which advertises its status via UDP heartbeats. The backup node (usually one, but there
 * can be more) waits for a cessation of heartbeats and assumes the role of primary. All nodes continuously ping a
 * well-known address (WKA), ideally a virtual IP that has failover of its own, to ensure that they have network
 * connectivity. This is so a backup node can know whether it has stopped receiving heartbeats because the primary node
 * has died or because it has lost its own connectivity to the network.
 * <p>
 * Each node can be in exactly one of the following {@link NodeState}s:
 * <ol>
 * <li>DISCONNECTED - The node cannot contact the WKA.</li>
 * <li>BACKUP - The node is connected to the network and receiving heartbeats from the primary.</li>
 * <li>VOLUNTEER - The node has volunteered to take over as primary.</li>
 * <li>PRIMARY - The node is currently active and working.</li>
 * </ol>
 * All nodes start up in the DISCONNECTED state, until they have confirmed network connectivity by pinging the WKA. Once
 * a ping response is received, the node transitions to the BACKUP state, and waits for heartbeats from a PRIMARY. If no
 * heartbeat is received within the configured timeout, the node transitions through VOLUNTEER to PRIMARY state and
 * begins sending heartbeats of its own.
 * <p>
 * If two or more nodes simultaneously attempt to become PRIMARY, they will receive each other's heartbeats. A
 * combination of start time and IP address is used to statically determine which node(s) should yield.
 * <p>
 * See <a href="http://code.google.com/p/guicebox/wiki/HotFailover">GuiceBox documentation</a> for details.
 * 
 * @author willhains
 */
@ThreadSafe public class Failover implements Cluster
{
	private final Logger _log;
	
	// Concurrency lock for cluster
	private final Object _clusterLock = new Object();
	
	// Application cluster name
	private final String _appName;
	private final String _env;
	
	// Heartbeat utility
	private final Provider<Heart> _heartFactory;
	@GuardedBy("_clusterLock") private Heart _heart;
	
	// Ping utility
	private final Provider<Ping> _pingFactory;
	@GuardedBy("_clusterLock") private Ping _ping;
	
	// Unique fingerprint of this node
	private final Node _node;
	
	// State of this node in the cluster (null when not participating in the cluster)
	@GuardedBy("_clusterLock") private NodeState _state;
	
	// Initial state of a node when it joins the cluster
	private final NodeState _initialState;
	
	@Inject Failover(
		@ApplicationName String appName,
		@UserName String env,
		Node node,
		Provider<Heart> heartFactory,
		Provider<Ping> pingFactory,
		Logger log)
	{
		this(appName, env, NodeState.Impl.DISCONNECTED, node, heartFactory, pingFactory, log);
	}
	
	// Should only be called from unit tests
	Failover(
		String appName,
		String env,
		NodeState initialState,
		Node node,
		Provider<Heart> heartFactory,
		Provider<Ping> pingFactory,
		Logger log)
	{
		_appName = appName;
		_env = env;
		_initialState = initialState;
		_node = node;
		_heartFactory = heartFactory;
		_pingFactory = pingFactory;
		_log = log;
	}
	
	@Override public String toString()
	{
		return _appName + " (" + _env + ")";
	}
	
	@GuardedBy("_clusterLock") private final Set<ClusterListener> _listeners = new HashSet<ClusterListener>();
	
	public void addListener(ClusterListener listener)
	{
		synchronized(_clusterLock)
		{
			_listeners.add(listener);
		}
	}
	
	public void removeListener(ClusterListener listener)
	{
		synchronized(_clusterLock)
		{
			_listeners.remove(listener);
		}
	}
	
	private void _changeState(NodeState newState)
	{
		synchronized(_clusterLock)
		{
			_state = newState;
			for(ClusterListener listener : _listeners)
			{
				listener.onClusterChange(newState == null ? null : newState.toString());
			}
		}
	}
	
	public void join(final Application app)
	{
		synchronized(_clusterLock)
		{
			_log.info("Joining cluster " + this);
			
			// Tolerate multiple calls to this method
			if(_state != null) return;
			
			// Initialise the state of the node
			_changeState(_initialState);
			_heart = _heartFactory.get();
			_ping = _pingFactory.get();
			
			// Start checking for network connectivity
			_ping.start(new PingListener()
			{
				public void onPing()
				{
					synchronized(_clusterLock)
					{
						_changeState(_state.onWkaAlive());
					}
				}
				
				public void onPingTimeout()
				{
					synchronized(_clusterLock)
					{
						_changeState(_state.onWkaDead(_heart, app));
					}
				}
			});
			
			// Start listening for heartbeats from the primary node
			_heart.listen(new HeartbeatListener()
			{
				public void onHeartbeat(final Heartbeat hb)
				{
					synchronized(_clusterLock)
					{
						_changeState(_state.onPeerAlive(_node, _heart, hb, app));
					}
				}
				
				public void onHeartbeatTimeout()
				{
					synchronized(_clusterLock)
					{
						_changeState(_state.onPeerDead(_heart, app));
					}
				}
			});
		}
	}
	
	public void leave()
	{
		synchronized(_clusterLock)
		{
			_log.info("Leaving cluster " + this);
			
			// Tolerate multiple calls to this method
			if(_state == null) return;
			
			// Stop sending & receiving heartbeats
			_heart.stop();
			
			// Stop checking for network connectivity
			_ping.stop();
			
			// No cluster state
			_changeState(null);
		}
	}
}
