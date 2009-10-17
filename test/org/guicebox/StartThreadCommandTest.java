package org.guicebox;

import static java.util.concurrent.TimeUnit.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;

/**
 * @author willhains
 */
public class StartThreadCommandTest
{
	// Mocks
	private Object[] _mocks;
	private ScheduledExecutorService _thread;
	private Runnable _runnable;
	
	@Start public Runnable simple;
	@Start("TestName2") public Runnable named;
	
	@Before public void createMocks()
	{
		_mocks = new Object[] {
			_thread = createMock(ScheduledExecutorService.class),
			_runnable = createMock(Runnable.class) };
	}
	
	@Test public void simpleThread() throws Throwable
	{
		final Field field = StartThreadCommandTest.class.getField("simple");
		final Start start = field.getAnnotation(Start.class);
		
		assertEquals("StartThreadCommandTest.simple", StartThreadCommand.buildThreadName(start.value(), field));
	}
	
	@Test public void namedThread() throws Throwable
	{
		final Field field = StartThreadCommandTest.class.getField("named");
		final Start start = field.getAnnotation(Start.class);
		
		assertEquals("TestName2", StartThreadCommand.buildThreadName(start.value(), field));
	}
	
	@Test public void commandNames() throws Throwable
	{
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 0);
		assertEquals("Start mock", cmd.toString());
		assertEquals("Stop mock", cmd.getStopCommand().toString());
		assertEquals("Kill mock", cmd.getKillCommand().toString());
	}
	
	@Test public void run() throws Throwable
	{
		expect(_thread.submit(_runnable)).andReturn(null);
		
		replay(_mocks);
		
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 0);
		cmd.call();
		
		verify(_mocks);
	}
	
	@Test public void repeating() throws Throwable
	{
		expect(_thread.scheduleAtFixedRate(_runnable, 0, 10, MILLISECONDS)).andReturn(null);
		
		replay(_mocks);
		
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 10);
		cmd.call();
		
		verify(_mocks);
	}
	
	@Test public void callAndStop() throws Throwable
	{
		expect(_thread.submit(_runnable)).andReturn(null);
		
		replay(_mocks);
		
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 0);
		cmd.call();
		cmd.getStopCommand().call();
		
		verify(_mocks);
	}
	
	@Test public void stopBeforeCall() throws Throwable
	{
		expect(_thread.submit(_runnable)).andReturn(null);
		
		replay(_mocks);
		
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 0);
		cmd.getStopCommand().call();
		cmd.call();
		
		verify(_mocks);
	}
	
	@Test public void killBeforeCall() throws Throwable
	{
		expect(_thread.shutdownNow()).andReturn(Collections.<Runnable> emptyList());
		expect(_thread.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)).andReturn(true);
		expect(_thread.submit(_runnable)).andReturn(null);
		
		replay(_mocks);
		
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 0);
		cmd.getKillCommand().call();
		cmd.getStopCommand().call();
		cmd.call();
		
		verify(_mocks);
	}
	
	@Test public void interruptJoin() throws Throwable
	{
		expect(_thread.submit(_runnable)).andReturn(null);
		expect(_thread.shutdownNow()).andReturn(Collections.<Runnable> emptyList());
		expect(_thread.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)).andThrow(new InterruptedException("faked"));
		
		replay(_mocks);
		
		final StartThreadCommand cmd = StartThreadCommand.create(_thread, _runnable, "mock", 0);
		cmd.call();
		Thread.currentThread().interrupt();
		cmd.getKillCommand().call();
		assertTrue(Thread.interrupted());
		
		verify(_mocks);
	}
}
