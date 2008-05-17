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
public final class GuiceBox
{
	// Start/Stop method invocations
	private static final List<Runnable> _startCommands = new LinkedList<Runnable>();
	private static final List<Runnable> _stopCommands = new LinkedList<Runnable>();
	
	// GuiceBox application state
	private static volatile GuiceBoxState _state = GuiceBoxState.NEW;
	
	// Single-threaded executor ensures that GuiceBox state is correct by thread confinement
	private static final ExecutorService _safe = Executors.newSingleThreadExecutor(new ThreadFactory()
	{
		@Override
		public Thread newThread(Runnable r)
		{
			return new Thread(r, "GuiceBox");
		}
	});
	
	// Application threads
	private static final List<Thread> _appThreads = new LinkedList<Thread>();
	
	/**
	 * Initialises the application with the specified Guice bindings. All the bound Guice classes that contain
	 * {@link Start} and/or {@link Stop} annotations will be bootstrapped. If anything goes wrong during bootstrapping,
	 * the application will be killed.
	 * 
	 * @param injector a Guice injector with bindings for startable/stoppable classes.
	 */
	public static void init(final Injector injector)
	{
		_safe.submit(new Runnable()
		{
			@Override
			public void run()
			{
				_state = _state.init(injector);
			}
		});
	}
	
	/**
	 * Starts the application by calling all the methods annotated with {@link Start}, and starting threads for all
	 * {@link Runnable}s annotated with {@link Start}.
	 */
	public static void start()
	{
		_safe.submit(new Runnable()
		{
			@Override
			public void run()
			{
				_state = _state.start();
			}
		});
	}
	
	/**
	 * Stops the application by calling all the methods annotated with {@link Stop}, and interrupting all threads
	 * started during {@link #start()}.
	 */
	public static void stop()
	{
		_safe.submit(new Runnable()
		{
			@Override
			public void run()
			{
				_state = _state.stop();
			}
		});
	}
	
	/**
	 * Kills the application.
	 */
	public static void kill()
	{
		_safe.submit(new Runnable()
		{
			@Override
			public void run()
			{
				_state = _state.kill();
			}
		});
	}
	
	private static List<Class<?>> getAllTypes(Class<?> type)
	{
		final List<Class<?>> allTypes = new ArrayList<Class<?>>();
		if(type != null)
		{
			allTypes.add(type);
			allTypes.addAll(Arrays.asList(type.getInterfaces()));
			allTypes.addAll(getAllTypes(type.getSuperclass()));
		}
		return allTypes;
	}
	
	// Control the behaviour depending on the state of the application
	private enum GuiceBoxState
	{
		NEW
		{
			@Override
			GuiceBoxState init(Injector injector)
			{
				try
				{
					// GuiceBox can only see classes that were specifically bound by the application's Modules (TODO: why?)
					for(final Binding<?> binding : injector.getBindings().values())
					{
						final Key<?> key = binding.getKey();
						final Type type = key.getTypeLiteral().getType();
						if(type instanceof Class)
						{
							final Class<?> impl = (Class<?>)type;
							final Method[] methods = impl.getMethods();
							Object instance = null;
							
							// Search for GuiceBox methods
							for(final Method method : methods)
							{
								List<Runnable> commands = null;
								if(method.getAnnotation(Start.class) != null) commands = _startCommands;
								if(method.getAnnotation(Stop.class) != null) commands = _stopCommands;
								if(commands != null)
								{
									// Check method for arguments
									if(method.getParameterTypes().length > 0)
									{
										throw new GuiceBoxException("Must have no arguments: " + method);
									}
									
									// Add a command to the list to call the GuiceBox method
									if(instance == null) instance = injector.getInstance(key);
									final Object o = instance;
									commands.add(new Runnable()
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
												GuiceBox.stop();
												GuiceBox.kill();
											}
										}
									});
									
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
									if(!getAllTypes(field.getType()).contains(Runnable.class))
									{
										throw new GuiceBoxException("@Start fields must be Runnable: " + field);
									}
									field.setAccessible(true);
									if(instance == null) instance = injector.getInstance(key);
									final Runnable runnable = (Runnable)field.get(instance);
									
									// Add a command to the list to create and/or start the thread
									_startCommands.add(new Runnable()
									{
										@Override
										public void run()
										{
											// Create the thread
											final Thread thread = new Thread(runnable);
											if(start.value().length() > 0) thread.setName(start.value());
											
											// Start the thread
											thread.start();
											_appThreads.add(thread);
										}
									});
								}
							}
						}
					}
					
					// Reverse the stop commands so that the application "backs out" when it stops
					Collections.reverse(_stopCommands);
					
					// Transition state
					return INITIALISED;
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					return INITIALISED.kill();
				}
			}
			
			@Override
			GuiceBoxState start()
			{
				throw new IllegalStateException("Application not initialised. Call init(Injector) first");
			}
			
			@Override
			GuiceBoxState stop()
			{
				throw new IllegalStateException("Application not initialised. Call init(Injector) first");
			}
			
			@Override
			GuiceBoxState kill()
			{
				_safe.shutdownNow();
				return this;
			}
		},
		
		INITIALISED
		{
			@Override
			GuiceBoxState init(Injector injector)
			{
				throw new IllegalStateException("Application already initialised. Call start() next.");
			}
			
			@Override
			GuiceBoxState start()
			{
				try
				{
					for(Runnable cmd : _startCommands)
					{
						cmd.run();
					}
					return STARTED;
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					return STARTED.stop().kill();
				}
			}
			
			@Override
			GuiceBoxState stop()
			{
				return this;
			}
			
			@Override
			GuiceBoxState kill()
			{
				_safe.shutdownNow();
				return this;
			}
		},
		
		STARTED
		{
			@Override
			GuiceBoxState init(Injector injector)
			{
				throw new IllegalStateException("Application already started. Call stop() next.");
			}
			
			@Override
			GuiceBoxState start()
			{
				throw new IllegalStateException("Application already started. Call stop() next.");
			}
			
			@Override
			GuiceBoxState stop()
			{
				try
				{
					// Interrupt application threads
					while(!_appThreads.isEmpty())
					{
						final Thread thread = _appThreads.remove(0);
						thread.interrupt();
					}
					
					// Run stop methods
					for(Runnable cmd : _stopCommands)
					{
						cmd.run();
					}
					return INITIALISED;
				}
				catch(Throwable e)
				{
					e.printStackTrace();
					return INITIALISED.kill();
				}
			}
			
			@Override
			GuiceBoxState kill()
			{
				return stop().kill();
			}
		};
		
		abstract GuiceBoxState init(Injector injector);
		
		abstract GuiceBoxState start();
		
		abstract GuiceBoxState stop();
		
		abstract GuiceBoxState kill();
	}
}
