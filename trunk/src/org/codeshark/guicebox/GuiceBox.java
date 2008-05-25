package org.codeshark.guicebox;

import com.google.inject.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Entry point to GuiceBox. Takes a Guice {@link Injector} and searches its bindings for classes annotated with
 * {@code @Start} and {@code @Stop} annotations. The lifecycle of the application is then controlled via these annotated
 * members.
 * 
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
	private volatile GuiceBoxState _state = GuiceBoxState.NEW;
	
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
	 * Equivalent to {@code injector.getInstance(GuiceBox.class).init()}.
	 * 
	 * @return the GuiceBox instance ready to start.
	 */
	public static GuiceBox init(Injector injector)
	{
		final GuiceBox guicebox = injector.getInstance(GuiceBox.class);
		guicebox.init();
		return guicebox;
	}
	
	/**
	 * Initialises the application with the specified Guice bindings. All the bound Guice classes that contain
	 * {@link Start}, {@link Stop} and/or {@link Kill} annotations will be bootstrapped. If anything goes wrong during
	 * bootstrapping, the application will be killed.
	 */
	public void init()
	{
		_safe.submit(new Runnable()
		{
			@Override
			public void run()
			{
				_state = _state.init(GuiceBox.this);
			}
		});
	}
	
	/**
	 * Starts the application by calling all the methods annotated with {@link Start}, and starting threads for all
	 * {@link Runnable}s annotated with {@link Start}.
	 */
	public void start()
	{
		final Runnable startTrigger = new Runnable()
		{
			public void run()
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
		};
		final Runnable stopTrigger = new Runnable()
		{
			public void run()
			{
				stop();
			}
		};
		_cluster.join(startTrigger, stopTrigger);
	}
	
	/**
	 * Stops the application by calling all the methods annotated with {@link Stop}, and interrupting all threads
	 * started during {@link #start()}.
	 */
	public void stop()
	{
		_cluster.leave();
		_safe.submit(new Runnable()
		{
			@Override
			public void run()
			{
				_state = _state.stop(GuiceBox.this);
			}
		});
	}
	
	/**
	 * Kills the application by calling all the methods annotated with {@link Kill}, and shutting down GuiceBox.
	 */
	public void kill()
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
					// GuiceBox can only see classes that were specifically bound by the application's Modules (TODO:
					// why?)
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
					return INITIALISED;
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					return INITIALISED.kill(guicebox);
				}
			}
			
			private void _searchClass(final GuiceBox guicebox, final Key<?> key, final Class<?> impl)
				throws IllegalAccessException
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
								e.printStackTrace();
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
			
			@Override
			GuiceBoxState start(GuiceBox guicebox)
			{
				throw new IllegalStateException("Application not initialised. Call init() first");
			}
			
			@Override
			GuiceBoxState stop(GuiceBox guicebox)
			{
				throw new IllegalStateException("Application not initialised. Call init() first");
			}
			
			@Override
			GuiceBoxState kill(GuiceBox guicebox)
			{
				return INITIALISED.kill(guicebox);
			}
		},
		
		INITIALISED
		{
			@Override
			GuiceBoxState init(GuiceBox guicebox)
			{
				throw new IllegalStateException("Application already initialised. Call start() next.");
			}
			
			@Override
			GuiceBoxState start(GuiceBox guicebox)
			{
				try
				{
					for(Runnable cmd : guicebox._startCommands)
					{
						cmd.run();
					}
					return STARTED;
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					return STARTED.stop(guicebox).kill(guicebox);
				}
			}
			
			@Override
			GuiceBoxState stop(GuiceBox guicebox)
			{
				return this;
			}
			
			@Override
			GuiceBoxState kill(GuiceBox guicebox)
			{
				guicebox._safe.shutdownNow();
				return this;
			}
		},
		
		STARTED
		{
			@Override
			GuiceBoxState init(GuiceBox guicebox)
			{
				throw new IllegalStateException("Application already started. Call stop() or kill() next.");
			}
			
			@Override
			GuiceBoxState start(GuiceBox guicebox)
			{
				throw new IllegalStateException("Application already started. Call stop() or kill() next.");
			}
			
			@Override
			GuiceBoxState stop(GuiceBox guicebox)
			{
				try
				{
					// Interrupt application threads
					while(!guicebox._appThreads.isEmpty())
					{
						final Thread thread = guicebox._appThreads.remove(0);
						thread.interrupt();
					}
					
					// Run stop methods
					for(Runnable cmd : guicebox._stopCommands)
					{
						cmd.run();
					}
					return INITIALISED;
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					return INITIALISED.kill(guicebox);
				}
			}
			
			@Override
			GuiceBoxState kill(GuiceBox guicebox)
			{
				return stop(guicebox).kill(guicebox);
			}
		};
		
		abstract GuiceBoxState init(GuiceBox guicebox);
		
		abstract GuiceBoxState start(GuiceBox guicebox);
		
		abstract GuiceBoxState stop(GuiceBox guicebox);
		
		abstract GuiceBoxState kill(GuiceBox guicebox);
	}
}
