package org.guicebox.failover;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import org.easymock.*;
import org.junit.*;

/**
 * @author willhains
 */
@SuppressWarnings("unchecked") public class JavaPingTest
{
	// Mocks
	private Object[] _mocks;
	private InetAddress _address1, _address2;
	private PingListener _listener;
	private ScheduledExecutorService _pingThread;
	private ScheduledFuture _pingTask;
	
	// Captures
	private Capture<Long> _initial;
	private Capture<Long> _period;
	private Capture<Runnable> _command;
	
	// Values
	private int _interval = 10;
	private int _tolerance = 3;
	
	@Before public void createMocks()
	{
		_mocks = new Object[] {
			_address1 = createMock(InetAddress.class),
			_address2 = createMock(InetAddress.class),
			_listener = createMock(PingListener.class),
			_pingThread = createMock(ScheduledExecutorService.class),
			_pingTask = createMock(ScheduledFuture.class),
		// Add all mocks here!
		};
		expect(_address1.getHostAddress()).andReturn("192.168.0.1").anyTimes();
		expect(_address2.getHostAddress()).andReturn("192.168.0.2").anyTimes();
		
		// Should schedule a ping command
		_command = new Capture<Runnable>();
		expect(
			_pingThread.scheduleWithFixedDelay(
				capture(_command),
				capture(_initial),
				capture(_period),
				eq(TimeUnit.MILLISECONDS))).andReturn(_pingTask);
		
	}
	
	@Before public void createCaptures()
	{
		_initial = new Capture<Long>();
		_period = new Capture<Long>();
	}
	
	private Ping _createPing()
	{
		return _createPing(Collections.singleton(_address1));
	}
	
	private Ping _createMultiPing()
	{
		return _createPing(new LinkedHashSet(Arrays.asList(_address1, _address2)));
	}
	
	private Ping _createPing(final Set<InetAddress> wkaSet)
	{
		final JavaPing ping = new JavaPing(wkaSet, _pingThread, Logger.getAnonymousLogger());
		ping.setPingInterval(_interval);
		ping.setPingTolerance(_tolerance);
		return ping;
	}
	
	@Test(expected = AssertionError.class) public void noWKAs()
	{
		_createPing(Collections.EMPTY_SET);
	}
	
	@Test(expected = AssertionError.class) public void invalidInterval()
	{
		_interval = 0;
		_createPing();
	}
	
	@Test(expected = AssertionError.class) public void invalidTolerance()
	{
		_tolerance = 0;
		_createPing();
	}
	
	@Test public void pingReachable() throws Exception
	{
		// Report a ping event for each successful ping 
		final int repeat = 2;
		expect(_address1.isReachable(_interval * _tolerance)).andReturn(true).times(repeat);
		_listener.onPing();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, (long)_initial.getValue());
		assertEquals(_interval, (long)_period.getValue());
		
		// Receive ping responses successfully
		for(int i = 0; i < repeat; i++)
		{
			_command.getValue().run();
		}
		
		// Stop pinging
		ping.stopPinging();
		verify(_mocks);
	}
	
	@Test public void pingUnreachable() throws Exception
	{
		// Report _tolerance times as many ping timeout events as failed pings
		final int repeat = 2;
		expect(_address1.isReachable(_interval * _tolerance)).andReturn(false).times(repeat * _tolerance);
		_listener.onPingTimeout();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, (long)_initial.getValue());
		assertEquals(_interval, (long)_period.getValue());
		
		// JavaPing task
		for(int i = 0; i < repeat; i++)
		{
			_command.getValue().run();
		}
		
		// Stop pinging
		ping.stopPinging();
		verify(_mocks);
	}
	
	@Test public void temporarilyUnreachable() throws Exception
	{
		// Address1 available for a while
		_address1.isReachable(_interval * _tolerance); // 1
		expectLastCall().andReturn(true);
		_listener.onPing();
		_address1.isReachable(_interval * _tolerance); // 2
		expectLastCall().andReturn(true);
		_listener.onPing();
		
		// Address1 becomes unavailable
		_address1.isReachable(_interval * _tolerance); // 3
		expectLastCall().andReturn(false);
		
		// Address2 is available for a while
		_address2.isReachable(_interval * _tolerance); // 3
		expectLastCall().andReturn(true);
		_listener.onPing();
		_address2.isReachable(_interval * _tolerance); // 4
		expectLastCall().andReturn(true);
		_listener.onPing();
		
		for(int i = 0; i < _tolerance; i++)
		{
			// Address2 fails also
			_address2.isReachable(_interval * _tolerance); // 5
			expectLastCall().andReturn(false);
			
			// So Address1 is tried again, but both are down
			_address1.isReachable(_interval * _tolerance); // 5
			expectLastCall().andReturn(false);
		}
		
		// The tolerance threshold is crossed
		_listener.onPingTimeout();
		
		// Address1 becomes available again
		_address2.isReachable(_interval * _tolerance); // 6
		expectLastCall().andReturn(false);
		_address1.isReachable(_interval * _tolerance); // 6
		expectLastCall().andReturn(true);
		_listener.onPing();
		_address1.isReachable(_interval * _tolerance); // 7
		expectLastCall().andReturn(true);
		_listener.onPing();
		
		// The ping is cancelled
		expect(_pingTask.cancel(true)).andReturn(true);
		
		// The End.
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createMultiPing();
		ping.start(_listener);
		assertEquals(0, (long)_initial.getValue());
		assertEquals(_interval, (long)_period.getValue());
		
		// JavaPing task
		for(int i = 0; i < 7; i++)
		{
			_command.getValue().run();
		}
		
		// Stop pinging
		ping.stopPinging();
		
		// The End.
		verify(_mocks);
	}
	
	@Test public void networkError() throws Exception
	{
		// Report network errors and timeout events
		int repeat = 2;
		_address1.isReachable(_interval * _tolerance);
		expectLastCall().andThrow(new IOException("Fake error")).times(repeat);
		_listener.onPingTimeout();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, (long)_initial.getValue());
		assertEquals(_interval, (long)_period.getValue());
		
		// JavaPing task
		for(int i = 0; i < repeat; i++)
		{
			_command.getValue().run();
		}
		
		// Stop pinging
		ping.stopPinging();
		verify(_mocks);
	}
	
	@Test public void stop() throws Exception
	{
		// Should shut down the ping thread and cancel the ping task
		expect(_pingThread.shutdownNow()).andAnswer(new _ShutdownAnswer());
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, (long)_initial.getValue());
		assertEquals(_interval, (long)_period.getValue());
		
		// Stop ping permanently
		ping.stop();
		
		// Simulate shutdown after task started running
		_command.getValue().run();
		verify(_mocks);
	}
	
	private static final class _ShutdownAnswer implements IAnswer<List<Runnable>>
	{
		public List<Runnable> answer() throws Throwable
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
