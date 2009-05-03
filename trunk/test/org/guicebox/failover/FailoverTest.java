package org.guicebox.failover;

import static org.easymock.EasyMock.*;

import com.google.inject.*;
import org.easymock.*;
import org.guicebox.*;
import org.junit.*;

/**
 * @author willhains
 */
public class FailoverTest
{
	// Mocks
	private Object[] _mocks;
	private Application _app;
	private NodeState _state;
	private Heart _heart;
	private Ping _ping;
	private Provider<Heart> _heartFactory;
	private Provider<Ping> _pingFactory;
	
	// Captures
	private Capture<PingListener> _pingListener;
	private Capture<HeartbeatListener> _hbListener;
	
	// Values
	private final Node _localhost = new Node("192.168.0.1", "PrimaryProcess");
	
	@Before @SuppressWarnings("unchecked") public void createMocks() throws Exception
	{
		_mocks = new Object[] {
			_app = createMock(Application.class),
			_state = createMock(NodeState.class),
			_heart = createMock(Heart.class),
			_ping = createMock(Ping.class),
			_heartFactory = createMock(Provider.class),
			_pingFactory = createMock(Provider.class),
		//
		};
		expect(_heartFactory.get()).andReturn(_heart).anyTimes();
		expect(_pingFactory.get()).andReturn(_ping).anyTimes();
		
		// Should start pinging and listening for heartbeats when joining the cluster
		_ping.start(capture(_pingListener));
		_heart.listen(capture(_hbListener));
	}
	
	@Before public void createCaptures()
	{
		_pingListener = new Capture<PingListener>();
		_hbListener = new Capture<HeartbeatListener>();
	}
	
	private Failover _joinCluster()
	{
		final Failover failover = new Failover(_state, _localhost, _heartFactory, _pingFactory);
		failover.join(_app);
		return failover;
	}
	
	@Test public void onPing()
	{
		// Should ping the WKA and pass the successful result to the node state
		expect(_state.onWkaAlive()).andReturn(_state);
		
		replay(_mocks);
		
		// Join the cluster
		_joinCluster();
		
		// Get a successful ping
		_pingListener.getValue().onPing();
		
		verify(_mocks);
	}
	
	@Test public void onPingTimeout()
	{
		// Should ping the WKA and pass the unsuccessful result to the node state
		expect(_state.onWkaDead(_heart, _app)).andReturn(_state);
		
		replay(_mocks);
		
		// Join the cluster
		_joinCluster();
		
		// Get a ping timeout
		_pingListener.getValue().onPingTimeout();
		
		verify(_mocks);
	}
	
	@Test public void onHeartbeat()
	{
		// Should receive a heartbeat from another node and pass the event to the node state
		final Node otherNode = new Node("192.168.0.2", "BackupProcess");
		final Heartbeat otherHB = new Heartbeat("FailoverTest", "TEST", otherNode);
		expect(_state.onPeerAlive(_localhost, _heart, otherHB, _app)).andReturn(_state);
		
		replay(_mocks);
		
		// Join the cluster
		_joinCluster();
		
		// Get a successful heartbeat
		_hbListener.getValue().onHeartbeat(otherHB);
		
		verify(_mocks);
	}
	
	@Test public void onHeartbeatTimeout()
	{
		// Should get a heartbeat timeout and pass the event to the node state
		expect(_state.onPeerDead(_heart, _app)).andReturn(_state);
		
		replay(_mocks);
		
		// Join the cluster
		_joinCluster();
		
		// Get a heartbeat timeout
		_hbListener.getValue().onHeartbeatTimeout();
		
		verify(_mocks);
	}
	
	@Test public void leaveCluster()
	{
		// Should stop the ping & heart when leaving the cluster
		_ping.stop();
		_heart.stop();
		
		replay(_mocks);
		
		// Join the cluster
		final Failover failover = _joinCluster();
		
		// Leave the cluster
		failover.leave();
		
		verify(_mocks);
	}
	
	@Test public void tolerateMultipleCalls()
	{
		// Should stop the ping & heart when leaving the cluster
		_ping.stop();
		_heart.stop();
		
		replay(_mocks);
		
		// Join the cluster multiple times
		final Failover failover = _joinCluster();
		failover.join(_app);
		failover.join(_app);
		
		// Leave the cluster
		failover.leave();
		failover.leave();
		failover.leave();
		
		verify(_mocks);
	}
	
	@Test public void becomePrimaryThenDisconnect()
	{
		_heart.beat();
		_app.start();
		_heart.stopBeating();
		_app.stop();
		_ping.stop();
		_heart.stop();
		
		replay(_mocks);
		
		final Failover failover = new Failover(_localhost, _heartFactory, _pingFactory);
		failover.join(_app);
		_pingListener.getValue().onPing();
		_hbListener.getValue().onHeartbeatTimeout();
		_hbListener.getValue().onHeartbeatTimeout();
		_pingListener.getValue().onPingTimeout();
		failover.leave();
		
		verify(_mocks);
	}
}
