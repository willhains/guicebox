package org.guicebox;

import static org.guicebox.NamedExecutors.*;

import java.lang.reflect.*;
import java.util.concurrent.*;

/**
 * Command object to spawn a thread in the future.
 * 
 * @author willhains
 */
final class StartThreadCommand implements Callable<Object>
{
	private final ExecutorService _thread;
	private Future<?> _task;
	private final Runnable _runnable;
	private final String _name;
	
	private StartThreadCommand(ExecutorService thread, Runnable runnable, String name)
	{
		_thread = thread;
		_runnable = runnable;
		_name = name;
	}
	
	public static StartThreadCommand create(Field field, String name, Object instance) throws GuiceBoxException
	{
		try
		{
			// Create the Runnable instance
			if(!Types.inheritedBy(field.getType()).contains(Runnable.class))
			{
				throw new GuiceBoxException("@Start fields must be Runnable: " + instance);
			}
			field.setAccessible(true);
			final Runnable runnable = (Runnable)field.get(instance);
			
			// Create the executor service
			final String threadName = buildThreadName(name, field);
			return new StartThreadCommand(newSingleThreadExecutor(threadName), runnable, threadName);
		}
		catch(IllegalAccessException e)
		{
			throw new GuiceBoxException("Cannot access @Start field: " + field, e);
		}
	}
	
	// Called by unit tests
	static StartThreadCommand externalExecutor(ExecutorService thread, Runnable runnable, String name)
	{
		return new StartThreadCommand(thread, runnable, name);
	}
	
	static String buildThreadName(String name, Field field)
	{
		return name == null || name.isEmpty()
			? field.getDeclaringClass().getSimpleName() + "." + field.getName()
			: name;
	}
	
	@Override public Object call()
	{
		_task = _thread.submit(_runnable);
		return null;
	}
	
	@Override public String toString()
	{
		return "Start " + _name;
	}
	
	public Callable<?> getStopCommand()
	{
		return new Callable<Object>()
		{
			@Override public Object call()
			{
				if(_task != null) _task.cancel(true);
				return null;
			}
			
			@Override public String toString()
			{
				return "Stop " + _name;
			}
		};
	}
	
	public Callable<?> getKillCommand()
	{
		return new Callable<Object>()
		{
			@Override public Object call() throws Exception
			{
				try
				{
					_thread.shutdownNow();
					_thread.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
				}
				catch(InterruptedException e)
				{
					// Restore interrupt status
					Thread.currentThread().interrupt();
				}
				return null;
			}
			
			@Override public String toString()
			{
				return "Kill " + _name;
			}
		};
	}
}
