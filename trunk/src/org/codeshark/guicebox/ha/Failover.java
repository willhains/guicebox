package org.codeshark.guicebox.ha;

import com.google.inject.*;
import org.codeshark.guicebox.*;

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
 * 
 * @author willhains
 */
public class Failover implements Cluster
{
	// Heartbeat utility
	private final Heart _heart;
	
	// Ping utility
	private final Ping _ping;
	
	// Unique fingerprint of this node
	private final Node _node;
	
	// State of this node in the cluster
	private NodeState _state;
	
	@Inject
	private Failover(Node node, Heart heart, Ping ping)
	{
		_node = node;
		_heart = heart;
		_ping = ping;
	}
	
	@Override
	public synchronized void join(final Runnable startTrigger, final Runnable stopTrigger)
	{
		// Tolerate multiple calls to this method
		if(_state != null) return;
		
		// Start off in a disconnected state
		_state = NodeState.DISCONNECTED;
		
		// Start checking for network connectivity
		_ping.start(new PingListener()
		{
			@Override
			public void onPing()
			{
				synchronized(Failover.this)
				{
					if(_state != null) _state = _state.onWkaAlive();
				}
			}
			
			@Override
			public void onPingTimeout()
			{
				synchronized(Failover.this)
				{
					if(_state != null) _state = _state.onWkaDead(_heart, stopTrigger);
				}
			}
		});
		
		// Start listening for heartbeats from the primary node
		_heart.listen(new HeartbeatListener()
		{
			@Override
			public void onHeartbeat(final Heartbeat hb)
			{
				synchronized(Failover.this)
				{
					if(_state != null) _state = _state.onHeartbeat(_node, _heart, hb, stopTrigger);
				}
			}
			
			@Override
			public void onHeartbeatTimeout()
			{
				synchronized(Failover.this)
				{
					if(_state != null) _state = _state.onPeerDead(_heart, startTrigger);
				}
			}
		});
	}
	
	@Override
	public synchronized void leave()
	{
		// Tolerate multiple calls to this method
		if(_state == null) return;
		
		// Stop sending & receiving heartbeats
		_heart.stopBeating();
		_heart.stopListening();
		
		// Stop checking for network connectivity
		_ping.stop();
		
		// No cluster state
		_state = null;
	}
}
