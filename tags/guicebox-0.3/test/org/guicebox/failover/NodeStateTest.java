package org.guicebox.failover;

import static org.easymock.EasyMock.*;
import static org.guicebox.failover.NodeState.*;
import static org.junit.Assert.*;

import org.guicebox.*;
import org.junit.*;

/**
 * @author willhains
 */
public class NodeStateTest
{
	private Heart _heart;
	private Application _app;
	private Node _primary, _standby;
	
	@Before public void createMocks()
	{
		_heart = createMock(Heart.class);
		_app = createMock(Application.class);
	}
	
	@Before public void createNodes()
	{
		_primary = new Node("1.1.1.100", "MockPrimary");
		_standby = new Node("1.1.1.101", "MockStandby");
	}
	
	@After public void verifyMocks()
	{
		verify(_heart, _app);
	}
	
	// Generate a new heartbeat from the primary
	private Heartbeat _hb(Node fromNode)
	{
		return new Heartbeat("NodeStateTest", "DEV", fromNode);
	}
	
	@Test public void startupToPrimary()
	{
		// Should start heartbeating, then start the app
		_heart.beat();
		_app.start();
		replay(_heart, _app);
		
		// Progress through 'normal' transition to primary
		NodeState state = DISCONNECTED;
		assertSame(STANDBY, state = state.onWkaAlive());
		assertSame(VOLUNTEER, state = state.onPeerDead(_heart, _app));
		assertSame(PRIMARY, state = state.onPeerDead(_heart, _app));
		assertSame(PRIMARY, state = state.onWkaAlive());
	}
	
	@Test public void startUpToStandby()
	{
		// Should be no calls to heart or app
		replay(_heart, _app);
		
		// Progress through 'normal' transition to standby
		NodeState state = DISCONNECTED;
		assertSame(STANDBY, state = state.onWkaAlive());
		assertSame(STANDBY, state = state.onPeerAlive(_standby, _heart, _hb(_primary), _app));
		assertSame(STANDBY, state = state.onWkaAlive());
	}
	
	@Test public void backupToPrimary()
	{
		// Should start heartbeating, then start the app
		_heart.beat();
		_app.start();
		replay(_heart, _app);
		
		// Standby takes over as primary
		NodeState state = STANDBY;
		assertSame(VOLUNTEER, state = state.onPeerDead(_heart, _app));
		assertSame(PRIMARY, state = state.onPeerDead(_heart, _app));
	}
	
	@Test public void failedVolunteer()
	{
		// Should start heartbeating, then stop heartbeating
		_heart.beat();
		_heart.stopBeating();
		replay(_heart, _app);
		
		// Standby volunteers to take over as primary, but a last-minute primary heartbeat stops it
		NodeState state = STANDBY;
		assertSame(VOLUNTEER, state = state.onPeerDead(_heart, _app));
		assertSame(STANDBY, state = state.onPeerAlive(_standby, _heart, _hb(_primary), _app));
	}
	
	@Test public void inferiorHeartbeat()
	{
		// Should start heartbeating, then send a forced heartbeat to overrule the inferior node, then start app
		_heart.beat();
		_heart.beat();
		_heart.beat();
		_app.start();
		replay(_heart, _app);
		
		// Standby volunteers as primary, and an inferior node also volunteers
		NodeState state = STANDBY;
		assertSame(VOLUNTEER, state = state.onPeerDead(_heart, _app));
		final Node secondStandby = new Node("1.1.1.200", "SecondStandby");
		assertSame(VOLUNTEER, state = state.onPeerAlive(_standby, _heart, _hb(secondStandby), _app));
		assertSame(VOLUNTEER, state = state.onWkaAlive());
		assertSame(PRIMARY, state = state.onPeerDead(_heart, _app));
		
		// Inferior node squeezes out one more heartbeat before yielding
		assertSame(PRIMARY, state = state.onPeerAlive(_standby, _heart, _hb(secondStandby), _app));
	}
	
	@Test public void superiorHeartbeat()
	{
		// Should stop heartbeating, and send a forced heartbeat to overrule the inferior node
		_heart.stopBeating();
		_app.stop();
		replay(_heart, _app);
		
		// Primary receives a heartbeat from a superior node
		NodeState state = PRIMARY;
		final Node superiorPrimary = new Node("1.1.1.1", "SuperiorPrimary");
		assertSame(STANDBY, state = state.onPeerAlive(_primary, _heart, _hb(superiorPrimary), _app));
	}
	
	@Test public void standbyDeath()
	{
		// Should be no calls ot heart or app
		replay(_heart, _app);
		
		// Standby stops heartbeating while the primary is active
		NodeState state = PRIMARY;
		assertSame(PRIMARY, state = state.onPeerDead(_heart, _app));
	}
	
	@Test public void heartbeatBeforePing()
	{
		// Should be no calls to heart or app
		replay(_heart, _app);
		
		// Receive a heartbeat from the primary before getting a ping result
		NodeState state = DISCONNECTED;
		assertSame(STANDBY, state = state.onPeerAlive(_standby, _heart, _hb(_primary), _app));
	}
	
	@Test public void heartbeatTimeoutBeforePing()
	{
		// Should be no calls to heart or app
		replay(_heart, _app);
		
		// Heartbeat times out before ping response
		NodeState state = DISCONNECTED;
		assertSame(DISCONNECTED, state = state.onPeerDead(_heart, _app));
		assertSame(DISCONNECTED, state = state.onWkaDead(_heart, _app));
	}
	
	@Test public void disconnectedWhileStandby()
	{
		// Should be no calls to heart or app
		replay(_heart, _app);
		
		// Ping fails while in standby state
		NodeState state = STANDBY;
		assertSame(DISCONNECTED, state = state.onWkaDead(_heart, _app));
	}
	
	@Test public void disconnectedWhileVolunteer()
	{
		// Should start heartbeating, then stop heartbeating
		_heart.beat();
		_heart.stopBeating();
		replay(_heart, _app);
		
		// Ping fails while volunteering to become primary
		NodeState state = STANDBY;
		assertSame(VOLUNTEER, state = state.onPeerDead(_heart, _app));
		assertSame(DISCONNECTED, state = state.onWkaDead(_heart, _app));
	}
	
	@Test public void disconnectedWhilePrimary()
	{
		// Should stop heartbeating, then stop the app
		_heart.stopBeating();
		_app.stop();
		replay(_heart, _app);
		
		// Ping fails while in primary state
		NodeState state = PRIMARY;
		assertSame(DISCONNECTED, state = state.onWkaDead(_heart, _app));
	}
}
