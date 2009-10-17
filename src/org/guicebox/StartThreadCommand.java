package org.guicebox;

import static java.util.concurrent.TimeUnit.*;
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
	private final ScheduledExecutorService _thread;
	private Future<?> _task;
	private final Runnable _runnable;
	private final String _name;
	private final long _interval;
	
	private StartThreadCommand(ScheduledExecutorService thread, Runnable runnable, String name, long repeatInterval)
	{
		_thread = thread;
		_runnable = runnable;
		_name = name;
		_interval = repeatInterval;
	}
	
	public static StartThreadCommand create(Field field, String name, long repeat, Object instance)
		throws GuiceBoxException
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
			return StartThreadCommand
				.create(newSingleThreadScheduledExecutor(threadName), runnable, threadName, repeat);
		}
		catch(IllegalAccessException e)
		{
			throw new GuiceBoxException("Cannot access @Start field: " + field, e);
		}
	}
	
	// Called by unit tests
	static StartThreadCommand create(ScheduledExecutorService thread, Runnable runnable, String name, long repeat)
	{
		return new StartThreadCommand(thread, runnable, name, repeat);
	}
	
	static String buildThreadName(String name, Field field)
	{
		if(name != null && name.length() > 0) return name;
		return field.getDeclaringClass().getSimpleName() + "." + field.getName();
	}
	
	public Object call()
	{
		_task = _interval > 0 //
			? _thread.scheduleAtFixedRate(_runnable, 0, _interval, MILLISECONDS)
			: _thread.submit(_runnable);
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
			public Object call()
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
			public Object call() throws Exception
			{
				try
				{
					_thread.shutdownNow();
					_thread.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
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
