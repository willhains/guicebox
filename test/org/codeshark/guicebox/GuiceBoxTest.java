package org.codeshark.guicebox;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.management.*;
import org.easymock.*;
import org.junit.*;

/**
 * @author willhains
 */
public class GuiceBoxTest
{
	// Mocks
	private Object[] _mocks;
	private Cluster _cluster;
	private CommandFactory _cmdFactory;
	private Callable<?> _start, _stop, _kill;
	private ExecutorService _gbThread;
	private ShutdownHook _hook;
	private MBeanServer _jmxServer;
	
	// Captures
	private Capture<Runnable> _shutdownTrigger;
	private Capture<Application> _app;
	@SuppressWarnings("unchecked") private final IAnswer<?> _appStart = new IAnswer()
	{
		@Override public Object answer() throws Throwable
		{
			_app.getValue().start();
			_app.reset();
			return null;
		}
	};
	
	@Before @SuppressWarnings("unchecked") public void createMocks()
	{
		_mocks = new Object[] {
			_cmdFactory = createMock(CommandFactory.class),
			_start = createMock(Callable.class),
			_stop = createMock(Callable.class),
			_kill = createMock(Callable.class),
			_cluster = createMock(Cluster.class),
			_gbThread = createMock(ExecutorService.class),
			_hook = createMock(ShutdownHook.class),
			_jmxServer = createMock(MBeanServer.class),
		// Add mocks here!
		};
		
		_app = new Capture<Application>();
		_shutdownTrigger = new Capture<Runnable>();
		_hook.add(eq("GuiceBox shutdown"), capture(_shutdownTrigger));
		
		final Capture<Runnable> runnable = new Capture<Runnable>();
		expect(_gbThread.submit(capture(runnable))).andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				runnable.getValue().run();
				runnable.reset();
				return null;
			}
		}).anyTimes();
	}
	
	private GuiceBox _newGuiceBox()
	{
		return new GuiceBox(_cmdFactory, _cluster, _gbThread, _hook, _jmxServer, Logger.getAnonymousLogger());
	}
	
	@Test public void start() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		
		verify(_mocks);
	}
	
	@Test public void stop() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.stop();
		
		verify(_mocks);
	}
	
	@Test public void clusterDrivenStartStop() throws Throwable
	{
		_cluster.join(capture(_app));
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		_app.getValue().start();
		_app.getValue().stop();
		
		verify(_mocks);
	}
	
	@Test public void clusterDrivenStartStopCycles() throws Throwable
	{
		_cluster.join(capture(_app));
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andReturn(null);
		expect(_gbThread.shutdownNow()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		_app.getValue().start();
		_app.getValue().stop();
		_app.getValue().start();
		_app.getValue().stop();
		gb.kill();
		
		verify(_mocks);
	}
	
	@Test public void clusterNeverStarts() throws Throwable
	{
		_cluster.join(capture(_app));
		_cluster.leave();
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andReturn(null);
		expect(_gbThread.shutdownNow()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.kill();
		
		verify(_mocks);
	}
	
	@Test public void kill() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andReturn(null);
		expect(_gbThread.shutdownNow()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.kill();
		
		verify(_mocks);
	}
	
	@Test public void stopUnstarted() throws Throwable
	{
		_cluster.leave();
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.stop();
		
		verify(_mocks);
	}
	
	@Test public void killUnstarted() throws Throwable
	{
		_cluster.leave();
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andReturn(null);
		expect(_gbThread.shutdownNow()).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.kill();
		
		verify(_mocks);
	}
	
	@Test public void stopStopped() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		_cluster.leave();
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.stop();
		gb.stop();
		
		verify(_mocks);
	}
	
	@Test public void startStarted() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.start();
		
		verify(_mocks);
	}
	
	@Test public void startError() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andThrow(new Exception("Fake error"));
		_cluster.leave();
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andReturn(null);
		expect(_gbThread.shutdownNow()).andReturn(Collections.<Runnable> emptyList());
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		
		verify(_mocks);
	}
	
	@Test public void stopError() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andThrow(new Exception("Fake error"));
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.stop();
		
		verify(_mocks);
	}
	
	@Test public void killError() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andThrow(new Exception("Fake error"));
		expect(_gbThread.shutdownNow()).andReturn(Collections.<Runnable> emptyList());
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		gb.stop();
		gb.kill();
		
		verify(_mocks);
	}
	
	@Test public void jvmShutdownHook() throws Throwable
	{
		_cluster.join(capture(_app));
		expectLastCall().andAnswer(_appStart);
		expect(_cmdFactory.getCommands(Start.class)).andReturn(Collections.<Callable<?>> singleton(_start));
		expect(_start.call()).andReturn(null);
		_cluster.leave();
		expect(_cmdFactory.getCommands(Stop.class)).andReturn(Collections.<Callable<?>> singleton(_stop));
		expect(_stop.call()).andReturn(null);
		expect(_cmdFactory.getCommands(Kill.class)).andReturn(Collections.<Callable<?>> singleton(_kill));
		expect(_kill.call()).andReturn(null);
		expect(_gbThread.shutdownNow()).andReturn(Collections.<Runnable> emptyList());
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.start();
		_shutdownTrigger.getValue().run();
		
		verify(_mocks);
	}
	
	@Test public void registerJMX() throws Throwable
	{
		final ObjectName name = new ObjectName("GuiceBox:name=GuiceBoxMBean");
		expect(_jmxServer.isRegistered(name)).andReturn(true);
		_jmxServer.unregisterMBean(name);
		final Capture<Object> object = new Capture<Object>();
		expect(_jmxServer.registerMBean(capture(object), eq(name))).andReturn(null);
		
		replay(_mocks);
		
		final GuiceBox gb = _newGuiceBox();
		gb.registerJMX();
		assertSame(gb, object.getValue());
		
		verify(_mocks);
	}
}
