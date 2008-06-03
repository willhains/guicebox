package org.codeshark.guicebox;

import com.google.inject.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;

/**
 * Entry point to GuiceBox. Takes a Guice {@link Injector} and searches its bindings for classes annotated with {@link
 * Start}, {@link Stop} and {@link Kill} annotations. The lifecycle of the application is then controlled via these
 * annotated members.
 * 
 * @see http://code.google.com/p/guicebox/wiki/GuiceBox
 * @author willhains
 */
@Singleton
public final class GuiceBox implements GuiceBoxMBean
{
	private final Log log = Log.forClass();
	
	// Start/Stop/Kill method invocations
	private final List<Runnable> _startCommands = new LinkedList<Runnable>();
	private final List<Runnable> _stopCommands = new LinkedList<Runnable>();
	private final List<Runnable> _killCommands = new LinkedList<Runnable>();
	
	// Single-threaded executor ensures that GuiceBox state is correct by thread confinement
	private final ExecutorService _safe;
	
	// GuiceBox application state
	private GuiceBoxState _state = GuiceBoxState.STOPPED;
	
	// Guice dependency injector
	private final Injector _injector;
	
	// Clustering scheme
	private final Cluster _cluster;
	
	@Inject
	private GuiceBox(Injector injector, Cluster cluster)
	{
		_injector = injector;
		_cluster = cluster;
		_safe = Executors.newSingleThreadExecutor(new ThreadFactory()
		{
			@Override
			public Thread newThread(Runnable r)
			{
				final Thread thread = new Thread(r, "GuiceBox");
				thread.setDaemon(false);
				return thread;
			}
		});
	}
	
	/**
	 * @see Guice#createInjector(Module...)
	 */
	public static GuiceBox init(Module... modules)
	{
		return init(Stage.DEVELOPMENT, modules);
	}
	
	/**
	 * @see Guice#createInjector(Iterable)
	 */
	public static GuiceBox init(Iterable<Module> modules)
	{
		return init(Stage.DEVELOPMENT, modules);
	}
	
	/**
	 * @see Guice#createInjector(Stage, Module...)
	 */
	public static GuiceBox init(Stage stage, Module... modules)
	{
		return init(stage, Arrays.asList(modules));
	}
	
	/**
	 * Initialises Guice and GuiceBox with the specified modules.
	 * 
	 * @return the GuiceBox instance ready to start.
	 * @see Guice#createInjector(Stage, Iterable)
	 */
	public static GuiceBox init(Stage stage, Iterable<Module> modules)
	{
		// Add an extra module for GuiceBox
		final List<Module> extraModules = new ArrayList<Module>();
		extraModules.add(new GuiceBoxModule());
		for(Module m : modules)
		{
			extraModules.add(m);
		}
		
		// Create a GuiceBox
		final GuiceBox guicebox = Guice.createInjector(stage, extraModules).getInstance(GuiceBox.class);
		guicebox._init();
		
		// Install a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread("GuiceBox shutdown")
		{
			@Override
			public void run()
			{
				guicebox.kill();
				System.out.flush();
				System.err.flush();
			}
		});
		
		return guicebox;
	}
	
	/**
	 * Initialises the application with the specified Guice bindings. All the bound Guice classes that contain {@link
	 * Start}, {@link Stop} and/or {@link Kill} annotations will be bootstrapped. If anything goes wrong during
	 * bootstrapping, the application will be killed.
	 */
	private void _init()
	{
		try
		{
			// GuiceBox can only see classes that were specifically bound by the application's Modules (TODO: why?)
			for(final Binding<?> binding : _injector.getBindings().values())
			{
				final Key<?> key = binding.getKey();
				final Type type = key.getTypeLiteral().getType();
				if(type instanceof Class)
				{
					// Will need an instance of each GuiceBoxed class to call its methods
					final Class<?> impl = (Class<?>)type;
					Object instance = null;
					
					// Search for GuiceBox methods
					final Method[] methods = impl.getMethods();
					for(final Method method : methods)
					{
						// Determine which GuiceBox annotations the method has, if any
						final List<Runnable> cmdList;
						if(method.getAnnotation(Start.class) != null) cmdList = _startCommands;
						else if(method.getAnnotation(Stop.class) != null) cmdList = _stopCommands;
						else if(method.getAnnotation(Kill.class) != null) cmdList = _killCommands;
						else continue;
						
						// Check method for arguments
						if(method.getParameterTypes().length > 0)
						{
							throw new GuiceBoxException("Must have no arguments: " + method);
						}
						
						// Create command to invoke method
						if(instance == null) instance = _injector.getInstance(key);
						final Object o = instance;
						cmdList.add(new InvokeMethodCommand(method, o));
					}
					
					// Search for GuiceBox fields
					for(final Field field : impl.getDeclaredFields())
					{
						// Add Runnable objects to the start list
						final Start start = field.getAnnotation(Start.class);
						if(start != null)
						{
							// Create the Runnable instance
							if(!_getAllTypes(field.getType()).contains(Runnable.class))
							{
								throw new GuiceBoxException("@Start fields must be Runnable: " + field);
							}
							field.setAccessible(true);
							if(instance == null) instance = _injector.getInstance(key);
							final Runnable runnable = (Runnable)field.get(instance);
							
							// Add commands to create, start, and stop the thread
							final ThreadStopCommand interrupt = new ThreadStopCommand();
							_startCommands.add(new ThreadStartCommand(interrupt, start, runnable));
							_stopCommands.add(interrupt);
						}
					}
				}
			}
			
			// Reverse the stop/kill commands so that the application "backs out" when it stops
			Collections.reverse(_stopCommands);
			Collections.reverse(_killCommands);
			
			// Transition state
			log.debug("GuiceBox INITIALISED");
		}
		catch(Throwable e)
		{
			log.exception(e);
			kill();
		}
	}
	
	private static List<Class<?>> _getAllTypes(Class<?> type)
	{
		final List<Class<?>> allTypes = new ArrayList<Class<?>>();
		if(type != null)
		{
			allTypes.add(type);
			allTypes.addAll(Arrays.asList(type.getInterfaces()));
			allTypes.addAll(_getAllTypes(type.getSuperclass()));
		}
		return allTypes;
	}
	
	private final class InvokeMethodCommand implements Runnable
	{
		private final Method _method;
		private final Object _instance;
		
		private InvokeMethodCommand(Method method, Object o)
		{
			_method = method;
			_instance = o;
		}
		
		@Override
		public void run()
		{
			try
			{
				_method.invoke(_instance);
			}
			catch(Exception e)
			{
				log.exception(e);
				kill();
			}
		}
	}
	
	private static final class ThreadStartCommand implements Runnable
	{
		private final ThreadStopCommand _interrupt;
		private final Start _start;
		private final Runnable _runnable;
		
		private ThreadStartCommand(ThreadStopCommand interrupt, Start start, Runnable runnable)
		{
			_interrupt = interrupt;
			_start = start;
			_runnable = runnable;
		}
		
		@Override
		public void run()
		{
			// Create the thread
			final Thread thread = new Thread(_runnable);
			if(_start.value().length() > 0) thread.setName(_start.value());
			_interrupt.setThread(thread);
			
			// Start the thread
			thread.start();
		}
	}
	
	private static final class ThreadStopCommand implements Runnable
	{
		private volatile Thread _thread;
		
		public void setThread(Thread t)
		{
			_thread = t;
		}
		
		@Override
		public void run()
		{
			try
			{
				_thread.interrupt();
				_thread.join();
			}
			catch(InterruptedException e)
			{
				// Die gracefully
				Thread.currentThread().interrupt();
			}
		}
	}
	
	// Triggers
	private final Runnable _startTrigger = _submission(new Runnable()
	{
		@Override
		public void run()
		{
			_state = _state.start(GuiceBox.this);
		}
	});
	private final Runnable _stopTrigger = _submission(new Runnable()
	{
		@Override
		public void run()
		{
			_state = _state.stop(GuiceBox.this);
		}
	});
	private final Runnable _killTrigger = _submission(new Runnable()
	{
		@Override
		public void run()
		{
			_state = _state.kill(GuiceBox.this);
		}
	});
	
	/**
	 * Starts the application by calling all the methods annotated with {@link Start}, and starting threads for all
	 * {@link Runnable}s annotated with {@link Start}.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void start()
	{
		// Join the cluster
		_cluster.join(_startTrigger, _stopTrigger);
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
		_stopTrigger.run();
	}
	
	/**
	 * Kills the application by calling all the methods annotated with {@link Kill}, and shutting down GuiceBox.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void kill()
	{
		// Ensure the application is already stopped
		stop();
		
		// Kill the application
		_killTrigger.run();
	}
	
	private Runnable _submission(final Runnable task)
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					_safe.submit(task);
				}
				catch(RejectedExecutionException e)
				{
					// Ignore
				}
			}
		};
	}
	
	/**
	 * Registers GuiceBox with JMX.
	 * 
	 * @throws JMException if the MBean couldn't be registered for some reason.
	 * @see ObjectName#ObjectName(String)
	 * @see MBeanServer#unregisterMBean(ObjectName)
	 * @see MBeanServer#registerMBean(Object, ObjectName)
	 * @see http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html#PasswordAccessFiles
	 */
	public void registerJMX() throws JMException
	{
		final MBeanServer srvr = ManagementFactory.getPlatformMBeanServer();
		final ObjectName objectName = new ObjectName("GuiceBox:name=" + GuiceBoxMBean.class.getSimpleName());
		if(srvr.isRegistered(objectName)) srvr.unregisterMBean(objectName);
		srvr.registerMBean(this, objectName);
	}
	
	// Control the behaviour depending on the state of the application
	private enum GuiceBoxState
	{
		STOPPED
		{
			@Override
			GuiceBoxState start(GuiceBox guicebox)
			{
				try
				{
					// Run start commands
					for(Runnable cmd : guicebox._startCommands)
					{
						cmd.run();
					}
					log.debug("GuiceBox STARTED");
					return STARTED;
				}
				catch(Throwable e)
				{
					log.exception(e);
					return STARTED.stop(guicebox).kill(guicebox);
				}
			}
			
			@Override
			GuiceBoxState stop(GuiceBox guicebox)
			{
				// Already stopped
				return this;
			}
			
			@Override
			GuiceBoxState kill(GuiceBox guicebox)
			{
				// Run kill commands
				for(Runnable cmd : guicebox._killCommands)
				{
					try
					{
						cmd.run();
					}
					catch(Throwable e)
					{
						// Log, but keep going
						log.exception(e);
					}
				}
				guicebox._safe.shutdownNow();
				log.info("GuiceBox KILLED");
				return this;
			}
		},
		
		STARTED
		{
			@Override
			GuiceBoxState start(GuiceBox guicebox)
			{
				return this;
			}
			
			@Override
			GuiceBoxState stop(GuiceBox guicebox)
			{
				try
				{
					// Run stop methods
					for(Runnable cmd : guicebox._stopCommands)
					{
						cmd.run();
					}
					log.debug("GuiceBox STOPPED");
					return STOPPED;
				}
				catch(Throwable e)
				{
					log.exception(e);
					return STOPPED.kill(guicebox);
				}
			}
			
			@Override
			GuiceBoxState kill(GuiceBox guicebox)
			{
				return stop(guicebox).kill(guicebox);
			}
		};
		
		protected final Log log = Log.forClass(GuiceBox.class);
		
		abstract GuiceBoxState start(GuiceBox guicebox);
		
		abstract GuiceBoxState stop(GuiceBox guicebox);
		
		abstract GuiceBoxState kill(GuiceBox guicebox);
	}
}
