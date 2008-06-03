package org.codeshark.guicebox;

import com.google.inject.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Entry point to GuiceBox. Takes a Guice {@link Injector} and searches its bindings for classes annotated with {@link
 * Start}, {@link Stop} and {@link Kill} annotations. The lifecycle of the application is then controlled via these
 * annotated members.
 * 
 * @see http://code.google.com/p/guicebox/wiki/GuiceBox
 * @author willhains
 */
@Singleton
public final class GuiceBox
{
	// Start/Stop/Kill method invocations
	private final List<Runnable> _startCommands = new LinkedList<Runnable>();
	private final List<Runnable> _stopCommands = new LinkedList<Runnable>();
	private final List<Runnable> _killCommands = new LinkedList<Runnable>();
	
	// Single-threaded executor ensures that GuiceBox state is correct by thread confinement
	private final ExecutorService _safe;
	
	// Application threads
	private final List<Thread> _appThreads = new LinkedList<Thread>();
	
	// GuiceBox application state
	private GuiceBoxState _state = GuiceBoxState.NEW;
	
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
				return new Thread(r, "GuiceBox");
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
		final List<Module> extraModules = new ArrayList<Module>();
		extraModules.add(new GuiceBoxModule());
		for(Module m : modules)
		{
			extraModules.add(m);
		}
		final Injector injector = Guice.createInjector(stage, extraModules);
		final GuiceBox guicebox = injector.getInstance(GuiceBox.class);
		guicebox.init();
		return guicebox;
	}
	
	/**
	 * Initialises the application with the specified Guice bindings. All the bound Guice classes that contain {@link
	 * Start}, {@link Stop} and/or {@link Kill} annotations will be bootstrapped. If anything goes wrong during
	 * bootstrapping, the application will be killed.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void init()
	{
		try
		{
			// Initialise the application
			_safe.submit(new Runnable()
			{
				@Override
				public void run()
				{
					_state = _state.init(GuiceBox.this);
				}
			});
		}
		catch(RejectedExecutionException e)
		{
			// Ignore
		}
	}
	
	/**
	 * Starts the application by calling all the methods annotated with {@link Start}, and starting threads for all
	 * {@link Runnable}s annotated with {@link Start}.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void start()
	{
		// Ensure the application is already initialised
		init();
		
		// Prepare triggers for start/stop
		final Runnable startTrigger = new Runnable()
		{
			public void run()
			{
				try
				{
					_safe.submit(new Runnable()
					{
						@Override
						public void run()
						{
							_state = _state.start(GuiceBox.this);
						}
					});
				}
				catch(RejectedExecutionException e)
				{
					// Ignore
				}
			}
		};
		final Runnable stopTrigger = new Runnable()
		{
			public void run()
			{
				try
				{
					_safe.submit(new Runnable()
					{
						@Override
						public void run()
						{
							_state = _state.stop(GuiceBox.this);
						}
					});
				}
				catch(RejectedExecutionException e)
				{
					// Ignore
				}
			}
		};
		
		// Join the cluster
		_cluster.join(startTrigger, stopTrigger);
	}
	
	/**
	 * Stops the application by calling all the methods annotated with {@link Stop}, and interrupting all threads
	 * started during {@link #start()}.
	 * <p>
	 * This method is non-blocking. It will return immediately.
	 */
	public void stop()
	{
		// Ensure the application is already initialised
		init();
		
		// Leave the cluster
		_cluster.leave();
		
		// Stop the application
		try
		{
			_safe.submit(new Runnable()
			{
				@Override
				public void run()
				{
					_state = _state.stop(GuiceBox.this);
				}
			});
		}
		catch(RejectedExecutionException e)
		{
			// Ignore
		}
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
		try
		{
			_safe.submit(new Runnable()
			{
				@Override
				public void run()
				{
					_state = _state.kill(GuiceBox.this);
				}
			});
		}
		catch(RejectedExecutionException e)
		{
			// Ignore
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
	
	// Control the behaviour depending on the state of the application
	private enum GuiceBoxState
	{
		NEW
		{
			@Override
			GuiceBoxState init(final GuiceBox guicebox)
			{
				try
				{
					// GuiceBox can only see classes that were specifically bound by the application's Modules
					// (TODO: why?)
					for(final Binding<?> binding : guicebox._injector.getBindings().values())
					{
						final Key<?> key = binding.getKey();
						final Type type = key.getTypeLiteral().getType();
						if(type instanceof Class) _searchClass(guicebox, key, (Class<?>)type);
					}
					
					// Reverse the stop/kill commands so that the application "backs out" when it stops
					Collections.reverse(guicebox._stopCommands);
					Collections.reverse(guicebox._killCommands);
					
					// Transition state
					log.debug("GuiceBox INITIALISED");
					return INITIALISED;
				}
				catch(Throwable e)
				{
					log.exception(e);
					return INITIALISED.kill(guicebox);
				}
			}
			
			private void _searchClass(final GuiceBox guicebox, Key<?> key, Class<?> impl) throws Exception
			{
				// Will need an instance of each GuiceBoxed class to call its methods
				Object instance = null;
				
				// Search for GuiceBox methods
				final Method[] methods = impl.getMethods();
				for(final Method method : methods)
				{
					// Determine which GuiceBox annotations the method has, if any
					final List<List<Runnable>> commandLists = new LinkedList<List<Runnable>>();
					if(method.getAnnotation(Start.class) != null) commandLists.add(guicebox._startCommands);
					if(method.getAnnotation(Stop.class) != null) commandLists.add(guicebox._stopCommands);
					if(method.getAnnotation(Kill.class) != null) commandLists.add(guicebox._killCommands);
					if(commandLists.isEmpty()) continue;
					
					// Check method for arguments
					if(method.getParameterTypes().length > 0)
					{
						throw new GuiceBoxException("Must have no arguments: " + method);
					}
					
					// Create command to invoke method
					if(instance == null) instance = guicebox._injector.getInstance(key);
					final Object o = instance;
					final Runnable command = new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								method.invoke(o);
							}
							catch(Exception e)
							{
								log.exception(e);
								kill(guicebox);
							}
						}
					};
					
					// Add the command to command queues
					for(List<Runnable> commands : commandLists)
					{
						commands.add(command);
					}
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
						if(instance == null) instance = guicebox._injector.getInstance(key);
						final Runnable runnable = (Runnable)field.get(instance);
						
						// Add a command to the list to create and/or start the thread
						guicebox._startCommands.add(new Runnable()
						{
							@Override
							public void run()
							{
								// Create the thread
								final Thread thread = new Thread(runnable);
								if(start.value().length() > 0) thread.setName(start.value());
								
								// Start the thread
								thread.start();
								guicebox._appThreads.add(thread);
							}
						});
					}
				}
			}
		},
		
		INITIALISED
		{
			@Override
			GuiceBoxState start(GuiceBox guicebox)
			{
				try
				{
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
			GuiceBoxState kill(GuiceBox guicebox)
			{
				guicebox._safe.shutdown();
				log.info("GuiceBox KILLED");
				return this;
			}
		},
		
		STARTED
		{
			@Override
			GuiceBoxState stop(GuiceBox guicebox)
			{
				try
				{
					// Interrupt application threads and wait for them to die
					while(!guicebox._appThreads.isEmpty())
					{
						final Thread thread = guicebox._appThreads.remove(0);
						thread.interrupt();
						thread.join();
					}
					
					// Run stop methods
					for(Runnable cmd : guicebox._stopCommands)
					{
						cmd.run();
					}
					log.debug("GuiceBox STOPPED");
					return INITIALISED;
				}
				catch(Throwable e)
				{
					log.exception(e);
					return INITIALISED.kill(guicebox);
				}
			}
		};
		
		protected final Log log = Log.forClass();
		
		GuiceBoxState init(@SuppressWarnings("unused") GuiceBox guicebox)
		{
			return this;
		}
		
		GuiceBoxState start(@SuppressWarnings("unused") GuiceBox guicebox)
		{
			return this;
		}
		
		GuiceBoxState stop(@SuppressWarnings("unused") GuiceBox guicebox)
		{
			return this;
		}
		
		GuiceBoxState kill(@SuppressWarnings("unused") GuiceBox guicebox)
		{
			return this;
		}
	}
}
