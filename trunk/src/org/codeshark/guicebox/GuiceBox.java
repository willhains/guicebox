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
	
	// TODO: Enforce valid state transitions (will be done as part of HA).
	
	/**
	 * Initialises the application with the specified Guice bindings. All the bound Guice classes that contain
	 * {@link Start} and/or {@link Stop} annotations will be bootstrapped. If anything goes wrong during bootstrapping,
	 * the application will be killed.
	 * 
	 * @param injector a Guice injector with bindings for startable/stoppable classes.
	 */
	public static void init(Injector injector)
	{
		// Initialise the command lists
		_startCommands = new LinkedList<Callable<?>>();
		_stopCommands = new LinkedList<Callable<?>>();
		
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
						if(field.getAnnotation(Start.class) != null)
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
		}
		catch(Throwable e)
		{
			e.printStackTrace();
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
	}
	
	/**
	 * Stops the application by calling all the methods annotated with {@link Stop}, and interrupting all threads
	 * started during {@link #start()}.
	 */
	public static void stop()
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
	}
	
	/**
	 * Kills the application.
	 */
	public static void kill()
	{ // TODO: This will stop the HA threads if they are running.
	}
}
