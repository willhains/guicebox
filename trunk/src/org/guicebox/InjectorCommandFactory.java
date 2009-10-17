package org.guicebox;

import com.google.inject.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scans the Guice {@link Injector} for implementation classes that contain the GuiceBox {@link Start}, {@link Stop} and
 * {@link Kill} annotations.
 * 
 * @author willhains
 */
public final class InjectorCommandFactory implements CommandFactory
{
	// Start/Stop/Kill method invocations
	private final Map<Class<? extends Annotation>, List<Callable<?>>> _commands;
	
	@Inject InjectorCommandFactory(Injector injector) throws GuiceBoxException
	{
		// Initialise command lists
		_commands = new HashMap<Class<? extends Annotation>, List<Callable<?>>>();
		_commands.put(Start.class, new LinkedList<Callable<?>>());
		_commands.put(Stop.class, new LinkedList<Callable<?>>());
		_commands.put(Kill.class, new LinkedList<Callable<?>>());
		
		// GuiceBox can only see classes that were specifically bound by the application's Modules
		for(final Binding<?> binding : injector.getBindings().values())
		{
			// Will need an instance of each GuiceBoxed class to call its methods
			final Key<?> key = binding.getKey();
			final Object instance = injector.getInstance(key);
			final Class<?> impl = instance.getClass();
			
			// Search for GuiceBox methods
			for(final Method method : impl.getDeclaredMethods())
			{
				// Determine which GuiceBox annotations the method has, if any
				for(Class<? extends Annotation> a : _commands.keySet())
				{
					// Create command to invoke method
					if(method.getAnnotation(a) != null) _commands.get(a).add(new InvokeMethodCommand(method, instance));
				}
			}
			
			// Search for GuiceBox fields
			for(final Field field : impl.getDeclaredFields())
			{
				// Add command objects to the start list
				final Start start = field.getAnnotation(Start.class);
				if(start != null)
				{
					// Create the thread
					final StartThreadCommand startCommand = StartThreadCommand.create( //
						field,
						start.value(),
						start.repeat(),
						instance);
					
					// Add commands to start, stop and kill the thread
					_commands.get(Start.class).add(startCommand);
					_commands.get(Stop.class).add(startCommand.getStopCommand());
					_commands.get(Kill.class).add(startCommand.getKillCommand());
				}
			}
		}
	}
	
	public Iterable<Callable<?>> getCommands(Class<? extends Annotation> transition)
	{
		return _commands.get(transition);
	}
}
