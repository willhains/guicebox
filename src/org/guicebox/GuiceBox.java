package org.guicebox;

import com.google.inject.*;
import java.lang.management.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.management.*;

/**
 * Entry point to GuiceBox. Takes a Guice {@link Injector} and searches its bindings for classes annotated with
 * {@link Start}, {@link Stop} and {@link Kill} annotations. The lifecycle of the application is then controlled via
 * these annotated members. See <a href="http://code.google.com/p/guicebox/wiki/GuiceBox">GuiceBox documentation</a> for
 * details.
 * 
 * @author willhains
 */
@Singleton public final class GuiceBox implements GuiceBoxMBean
{
	private final Logger _log;
	
	// Single-threaded executor ensures that GuiceBox state is correct by thread confinement
	private final ExecutorService _gbThread;
	
	// GuiceBox application state
	private GuiceBoxState _state = GuiceBoxState.STOPPED;
	
	// Clustering scheme
	private final Cluster _cluster;
	
	// GuiceBox command factory
	private final CommandFactory _commandFactory;
	
	@Inject GuiceBox(CommandFactory commandFactory, Cluster cluster, Logger log)
	{
		this(
			commandFactory,
			cluster,
			NamedExecutors.newSingleThreadExecutor("GuiceBox"),
			new ShutdownHookAdapter(),
			ManagementFactory.getPlatformMBeanServer(),
			log);
	}
	
	// Called by unit tests
	GuiceBox(
		CommandFactory commandFactory,
		Cluster cluster,
		ExecutorService gbThread,
		ShutdownHook hook,
		MBeanServer jmxServer,
		Logger log)
	{
		_log = log;
		_cluster = cluster;
		_commandFactory = commandFactory;
		_gbThread = gbThread;
		_jmxServer = jmxServer;
		
		// Install a shutdown hook
		hook.add("GuiceBox shutdown", new Runnable()
		{
			@Override public void run()
			{
				kill();
				System.out.flush();
				System.err.flush();
			}
		});
		
		// Transition state
		_log.finer("GuiceBox INITIALISED");
	}
	
	// Triggers to schedule start, stop, kill on the GuiceBox thread
	private final Runnable _startTrigger = new Runnable()
	{
		@Override public void run()
		{
			try
			{
				_state = _state.start(_commandFactory);
			}
			catch(Throwable e)
			{
				_log.severe("GuiceBox could not start: " + e);
				kill();
			}
			
		}
	};
	private final Runnable _stopTrigger = new Runnable()
	{
		@Override public void run()
		{
			_state = _state.stop(_commandFactory);
		}
	};
	private final Runnable _killTrigger = new Runnable()
	{
		@Override public void run()
		{
			_state = _state.kill(_commandFactory);
			_gbThread.shutdownNow();
		}
	};
	
	private final MBeanServer _jmxServer;
	
	/**
	 * Starts the application by calling all the methods annotated with {@link Start}, and starting threads for all
	 * {@link Runnable}s annotated with {@link Start}.
	 * <p>
	 * This method is non-blocking. It will return immediately. The {@link @Start} methods/threads will be started
	 * asynchronously.
	 */
	public void start()
	{
		// Join the cluster
		_cluster.join(new Application()
		{
			@Override public void start()
			{
				_gbThread.submit(_startTrigger);
			}
			
			@Override public void stop()
			{
				_gbThread.submit(_stopTrigger);
			}
		});
	}
	
	/**
	 * Stops the application by calling all the methods annotated with {@link Stop}, and interrupting all threads
	 * started during {@link #start()}.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void stop()
	{
		// Leave the cluster
		_cluster.leave();
		
		// Stop the application
		_gbThread.submit(_stopTrigger);
	}
	
	/**
	 * Kills the application by calling all the methods annotated with {@link Kill}, and shutting down GuiceBox.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void kill()
	{
		// Leave the cluster
		_cluster.leave();
		
		// Kill the application
		_gbThread.submit(_killTrigger);
	}
	
	/**
	 * Registers GuiceBox with JMX. See <a
	 * href="http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html#PasswordAccessFiles">JMX Documentation</a>
	 * for details on how to secure access.
	 * 
	 * @throws JMException if the MBean couldn't be registered for some reason.
	 * @see ObjectName#ObjectName(String)
	 * @see MBeanServer#unregisterMBean(ObjectName)
	 * @see MBeanServer#registerMBean(Object, ObjectName)
	 */
	public void registerJMX() throws JMException
	{
		final ObjectName objectName = new ObjectName("GuiceBox:name=" + GuiceBoxMBean.class.getSimpleName());
		if(_jmxServer.isRegistered(objectName)) _jmxServer.unregisterMBean(objectName);
		_jmxServer.registerMBean(this, objectName);
	}
}

interface ShutdownHook
{
	void add(String name, Runnable hook);
}

final class ShutdownHookAdapter implements ShutdownHook
{
	public void add(String name, Runnable hook)
	{
		Runtime.getRuntime().addShutdownHook(new Thread(hook, name));
	}
}
