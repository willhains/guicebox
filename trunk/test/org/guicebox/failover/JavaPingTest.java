package org.guicebox.failover;

import static java.util.concurrent.TimeUnit.*;
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
	private InetAddress _address;
	private PingListener _listener;
	private ScheduledExecutorService _pingThread;
	private ScheduledFuture _pingTask;
	
	// Captures
	private Capture<Integer> _initial;
	private Capture<Integer> _period;
	private Capture<Runnable> _command;
	
	// Values
	private int _interval = 10;
	private int _tolerance = 3;
	
	@Before public void createMocks()
	{
		_mocks = new Object[] {
			_address = createMock(InetAddress.class),
			_listener = createMock(PingListener.class),
			_pingThread = createMock(ScheduledExecutorService.class),
			_pingTask = createMock(ScheduledFuture.class),
		// Add all mocks here!
		};
		expect(_address.getHostAddress()).andReturn("192.168.0.100").anyTimes();
		
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
		_initial = new Capture<Integer>();
		_period = new Capture<Integer>();
	}
	
	private Ping _createPing()
	{
		final JavaPing ping = new JavaPing(_address, _pingThread, Logger.getAnonymousLogger());
		ping.setPingInterval(_interval);
		ping.setPingTolerance(_tolerance);
		return ping;
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
		expect(_address.isReachable(_interval * _tolerance)).andReturn(true).times(repeat);
		_listener.onPing();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, _initial.getValue());
		assertEquals(_interval, _period.getValue());
		
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
		expect(_address.isReachable(_interval * _tolerance)).andReturn(false).times(repeat * _tolerance);
		_listener.onPingTimeout();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, _initial.getValue());
		assertEquals(_interval, _period.getValue());
		
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
		// Report ping events and timeout events
		int repeat = 2;
		_address.isReachable(_interval * _tolerance);
		expectLastCall().andReturn(true).times(repeat);
		expectLastCall().andReturn(false).times(repeat * _tolerance);
		expectLastCall().andReturn(true).times(repeat);
		_listener.onPing();
		expectLastCall().times(repeat * 2);
		_listener.onPingTimeout();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, _initial.getValue());
		assertEquals(_interval, _period.getValue());
		
		// JavaPing task
		for(int i = 0; i < repeat * 3; i++)
		{
			_command.getValue().run();
		}
		
		// Stop pinging
		ping.stopPinging();
		verify(_mocks);
	}
	
	@Test public void networkError() throws Exception
	{
		// Report network errors and timeout events
		int repeat = 2;
		_address.isReachable(_interval * _tolerance);
		expectLastCall().andThrow(new IOException("Fake error")).times(repeat * _tolerance);
		_listener.onPingTimeout();
		expectLastCall().times(repeat);
		
		// Should cancel the ping command
		expect(_pingTask.cancel(true)).andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, _initial.getValue());
		assertEquals(_interval, _period.getValue());
		
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
		_pingThread.awaitTermination(_interval, MILLISECONDS);
		expectLastCall().andReturn(false).times(2);
		expectLastCall().andReturn(true);
		replay(_mocks);
		
		// Start ping
		final Ping ping = _createPing();
		ping.start(_listener);
		assertEquals(0, _initial.getValue());
		assertEquals(_interval, _period.getValue());
		
		// Stop ping permanently
		ping.stop();
		
		// Simulate shutdown after task started running
		_command.getValue().run();
		verify(_mocks);
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
