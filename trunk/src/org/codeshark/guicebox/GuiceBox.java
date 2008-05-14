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
	private static List<Callable<?>> _startCommands;
	private static List<Callable<?>> _stopCommands;
	
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
	
	/**
	 * Initialises the application with the specified Guice bindings. All the bound Guice classes that contain
	 * {@link Start} and/or {@link Stop} annotations will be bootstrapped. If anything goes wrong during bootstrapping,
	 * the application will be killed.
	 * 
	 * @param injector a Guice injector with bindings for startable/stoppable classes.
	 */
	public static void init(final Injector injector)
	{
		try
		{
			_schedule(new Callable<GuiceBoxState>()
			{
				@Override
				public GuiceBoxState call() throws Exception
				{
					return _state.init(injector);
				}
			});
		}
		catch(Throwable e)
		{
			kill();
		}
	}
	
	/**
	 * Starts the application by calling all the methods annotated with {@link Start}, and starting threads for all
	 * {@link Runnable}s annotated with {@link Start}.
	 */
	public static void start()
	{
		try
		{
			_schedule(new Callable<GuiceBoxState>()
			{
				@Override
				public GuiceBoxState call() throws Exception
				{
					return _state.start();
				}
			});
		}
		catch(Throwable e)
		{
			stop();
			kill();
		}
	}
	
	/**
	 * Stops the application by calling all the methods annotated with {@link Stop}, and interrupting all threads
	 * started during {@link #start()}.
	 */
	public static void stop()
	{
		try
		{
			_schedule(new Callable<GuiceBoxState>()
			{
				@Override
				public GuiceBoxState call() throws Exception
				{
					return _state.stop();
				}
			});
		}
		catch(Throwable e)
		{
			kill();
		}
	}
	
	/**
	 * Kills the application.
	 */
	public static void kill()
	{
		try
		{
			_schedule(new Callable<GuiceBoxState>()
			{
				@Override
				public GuiceBoxState call() throws Exception
				{
					return _state.kill();
				}
			});
		}
		catch(Throwable e)
		{
			System.exit(1);
		}
	}
	
	private static void _schedule(final Callable<GuiceBoxState> task) throws Throwable
	{
		try
		{
			_state = _safe.submit(task).get();
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		catch(ExecutionException e)
		{
			e.getCause().printStackTrace();
			throw e.getCause();
		}
	}
	
	// Control the behaviour depending on the state of the application
	private enum GuiceBoxState
	{
		NEW
		{
			@Override
			GuiceBoxState init(Injector injector) throws Exception
			{
				// Initialise the command lists
				_startCommands = new LinkedList<Callable<?>>();
				_stopCommands = new LinkedList<Callable<?>>();
				
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
							List<Callable<?>> commands = null;
							if(method.getAnnotation(Start.class) != null) commands = _startCommands;
							if(method.getAnnotation(Stop.class) != null) commands = _stopCommands;
							if(commands != null)
							{
								// Check method for arguments
								if(method.getParameterTypes().length > 0)
								{
									throw new GuiceBoxException("GuiceBox methods must have no arguments: " + method);
								}
								
								// Add a command to the list to call the GuiceBox method
								if(instance == null) instance = injector.getInstance(key);
								final Object o = instance;
								commands.add(new Callable<Object>()
								{
									@Override
									public Object call() throws Exception
									{
										return method.invoke(o);
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
								field.setAccessible(true);
								if(instance == null) instance = injector.getInstance(key);
								final Object runnable = field.get(instance);
								
								// Check field for runnability
								if(!Runnable.class.isInstance(runnable))
								{
									throw new GuiceBoxException("@Start fields must be Runnable: " + field);
								}
								
								// Create the thread if necessary
								final Thread thread = Thread.class.isInstance(runnable)
								    ? (Thread)runnable
								    : new Thread((Runnable)runnable);
								if(start.value().length() > 0) thread.setName(start.value());
								
								// Add a command to the list to create and/or start the thread
								_startCommands.add(new Callable<Object>()
								{
									@Override
									public Object call() throws Exception
									{
										thread.start();
										return null;
									}
								});
								
								// Add a command to the list to interrupt the thread
								_stopCommands.add(new Callable<Object>()
								{
									@Override
									public Object call() throws Exception
									{
										thread.interrupt();
										return null;
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
			
			@Override
			GuiceBoxState start() throws Exception
			{
				throw new IllegalStateException("Application not initialised. Call init(Injector) first");
			}
			
			@Override
			GuiceBoxState stop() throws Exception
			{
				throw new IllegalStateException("Application not initialised. Call init(Injector) first");
			}
			
			@Override
			GuiceBoxState kill() throws Exception
			{
				_safe.shutdownNow();
				return this;
			}
		},
		
		INITIALISED
		{
			@Override
			GuiceBoxState init(Injector injector) throws Exception
			{
				throw new IllegalStateException("Application already initialised. Call start() next.");
			}
			
			@Override
			GuiceBoxState start() throws Exception
			{
				try
				{
					for(Callable<?> cmd : _startCommands)
					{
						cmd.call();
					}
				}
				catch(Exception e)
				{
					e.getCause().printStackTrace();
					stop();
					kill();
				}
				return STARTED;
			}
			
			@Override
			GuiceBoxState stop() throws Exception
			{
				return this;
			}
			
			@Override
			GuiceBoxState kill() throws Exception
			{
				_safe.shutdownNow();
				return this;
			}
		},
		
		STARTED
		{
			@Override
			GuiceBoxState init(Injector injector) throws Exception
			{
				throw new IllegalStateException("Application already started. Call stop() next.");
			}
			
			@Override
			GuiceBoxState start() throws Exception
			{
				throw new IllegalStateException("Application already started. Call stop() next.");
			}
			
			@Override
			GuiceBoxState stop() throws Exception
			{
				try
				{
					for(Callable<?> cmd : _stopCommands)
					{
						cmd.call();
					}
				}
				catch(Exception e)
				{
					e.getCause().printStackTrace();
					kill();
				}
				return INITIALISED;
			}
			
			@Override
			GuiceBoxState kill() throws Exception
			{
				return stop().kill();
			}
		};
		
		abstract GuiceBoxState init(Injector injector) throws Exception;
		
		abstract GuiceBoxState start() throws Exception;
		
		abstract GuiceBoxState stop() throws Exception;
		
		abstract GuiceBoxState kill() throws Exception;
	}
}
