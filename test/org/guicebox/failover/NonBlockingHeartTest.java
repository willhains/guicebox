package org.guicebox.failover;

import static java.util.concurrent.TimeUnit.*;
import static org.easymock.EasyMock.*;

import com.google.inject.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import org.easymock.*;
import org.junit.*;

/**
 * @author willhains
 */
@SuppressWarnings("unchecked") public class NonBlockingHeartTest
{
	// Mocks
	private Object[] _mocks;
	private Transport _transport;
	private HeartbeatListener _listener;
	private Provider<Heartbeat> _pulse, _otherPulse;
	private ScheduledExecutorService _listenThread, _beatThread;
	
	// Values
	private final Node _localhost = new Node("127.0.0.1", "HeartTestProcess");
	private final Node _peer = new Node("1.1.1.1", "HeartTestPeer");
	private final Heartbeat _ownHeartbeat = new Heartbeat("NonBlockingHeartTest", "TEST", _localhost);
	private final Heartbeat _peerHeartbeat = new Heartbeat("NonBlockingHeartTest", "TEST", _peer);
	private int _interval = 10;
	private int _tolerance = 3;
	
	@Before public void createMocks()
	{
		_mocks = new Object[] {
			_transport = createMock(Transport.class),
			_listener = createMock(HeartbeatListener.class),
			_pulse = createMock(Provider.class),
			_otherPulse = createMock(Provider.class),
			_listenThread = createMock(ScheduledExecutorService.class),
			_beatThread = createMock(ScheduledExecutorService.class) };
		expect(_pulse.get()).andReturn(new Heartbeat("NonBlockingHeartTest", "TEST", _localhost)).anyTimes();
		expect(_otherPulse.get()).andReturn(new Heartbeat("NonBlockingHeartTest", "TEST", _peer)).anyTimes();
	}
	
	private Heart _createHeart()
	{
		final NonBlockingHeart heart = new NonBlockingHeart(_pulse, _transport, _listenThread, _beatThread, Logger
			.getAnonymousLogger());
		heart.setHeartbeatInterval(_interval);
		heart.setHeartbeatTolerance(_tolerance);
		return heart;
	}
	
	@Test(expected = AssertionError.class) public void invalidInterval()
	{
		_interval = 0;
		_createHeart();
	}
	
	@Test(expected = AssertionError.class) public void invalidTolerance()
	{
		_tolerance = 0;
		_createHeart();
	}
	
	// TODO: Split these tests up to test individual functionality like JavaPingTest
	// TODO: Should not depend on internal implementation of failure counting
	@Test public void listen() throws Exception
	{
		// Should schedule a listen command
		final ScheduledFuture lstnTask = createMock(ScheduledFuture.class);
		final Capture<Runnable> command = new Capture<Runnable>();
		expect(_listenThread.scheduleWithFixedDelay(capture(command), eq(0L), eq(1L), eq(MILLISECONDS))) //
			.andReturn(lstnTask);
		
		// Should try to receive heartbeats - succeed 2 times, timeout 10 times, fail 8 times
		_transport.receive(_ownHeartbeat, _interval);
		expectLastCall().andReturn(_peerHeartbeat).times(2);
		expectLastCall().andThrow(new TimeoutException()).times(10);
		expectLastCall().andThrow(new TransportException("Fake error")).times(8);
		
		// Should fire a heartbeat event 2 times, and a timeout event 6 times (due to tolerance)
		_listener.onHeartbeat(_peerHeartbeat);
		expectLastCall().times(2);
		_listener.onHeartbeatTimeout();
		expectLastCall().times(6);
		
		// Should cancel the listen command
		expect(lstnTask.cancel(true)).andReturn(true);
		replay(_mocks);
		replay(lstnTask);
		
		// Listen for heartbeats
		final Heart heart = _createHeart();
		heart.listen(_listener);
		
		// Receive 2 heartbeats successfully
		command.getValue().run();
		command.getValue().run();
		
		// 10 timeouts + 8 failures = 18 with tolerance of 3 means 6 runs and 6 events
		command.getValue().run();
		command.getValue().run();
		command.getValue().run();
		command.getValue().run();
		command.getValue().run();
		command.getValue().run();
		
		// Stop listening
		heart.stopListening();
		verify(_mocks);
		verify(lstnTask);
	}
	
	@Test public void beat() throws Exception
	{
		// Should schedule a beat _command
		final ScheduledFuture beatTask = createMock(ScheduledFuture.class);
		final Capture<Runnable> command = new Capture<Runnable>();
		expect(_beatThread.scheduleAtFixedRate(capture(command), eq(0L), eq((long)_interval), eq(MILLISECONDS)))
			.andReturn(beatTask);
		
		// Should try to send heartbeats - succeed 2 times, fail 3 times
		_transport.send(_ownHeartbeat);
		expectLastCall().times(2);
		expectLastCall().andThrow(new TransportException("Fake error")).times(3);
		
		// Should cancel the beat command
		expect(beatTask.cancel(true)).andReturn(true);
		replay(_mocks);
		replay(beatTask);
		
		// Send heartbeats
		final Heart heart = _createHeart();
		heart.beat();
		for(int i = 0; i < 3; i++)
		{
			command.getValue().run();
		}
		verify(_mocks);
		verify(beatTask);
	}
	
	@Test public void stop() throws Exception
	{
		// Should schedule beat & listen commands 
		final ScheduledFuture beatTask = createMock(ScheduledFuture.class);
		final ScheduledFuture lstnTask = createMock(ScheduledFuture.class);
		final Capture<Runnable> beatCommand = new Capture<Runnable>();
		final Capture<Runnable> lstnCommand = new Capture<Runnable>();
		expect(_beatThread.scheduleAtFixedRate(capture(beatCommand), eq(0L), eq((long)_interval), eq(MILLISECONDS))) //
			.andReturn(beatTask);
		expect(_listenThread.scheduleWithFixedDelay(capture(lstnCommand), eq(0L), eq(1L), eq(MILLISECONDS))) //
			.andReturn(lstnTask);
		
		// Should shut down the schedulers and cancel the beat & listen commands
		expect(_beatThread.shutdownNow()).andAnswer(new _ShutdownAnswer());
		expect(_listenThread.shutdownNow()).andAnswer(new _ShutdownAnswer());
		_beatThread.awaitTermination(_interval, MILLISECONDS);
		expectLastCall().andReturn(false).times(2);
		expectLastCall().andReturn(true);
		_listenThread.awaitTermination(_interval, MILLISECONDS);
		expectLastCall().andReturn(false).times(2);
		expectLastCall().andReturn(true);
		replay(_mocks);
		replay(beatTask, lstnTask);
		
		// Send heartbeats and listen for heartbeats
		final Heart heart = _createHeart();
		heart.beat();
		heart.listen(_listener);
		
		// Stop the heart
		heart.stop();
		
		// Simulate shutdown after tasks started running
		beatCommand.getValue().run();
		lstnCommand.getValue().run();
		verify(_mocks);
		verify(beatTask, lstnTask);
	}
	
	private static final class _ShutdownAnswer implements IAnswer<List<Runnable>>
	{
		@Override public List<Runnable> answer() throws Throwable
		{
			Thread.currentThread().interrupt();
			return Collections.<Runnable> emptyList();
		}
	}
	
	@After public void clearInterrupt()
	{
		// Clear thread interrupt flag to avoid impact on other tests
		Thread.interrupted();
	}
}
