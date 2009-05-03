package org.guicebox;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Convenience class to get around the annoying feature of {@link Executors} that forces you to create a whole
 * {@link ThreadFactory} just to give your threads a name.
 * 
 * @author willhains
 */
public class NamedExecutors
{
	private NamedExecutors()
	{
		// Utility class
	}
	
	/**
	 * @return a {@link ThreadFactory} that produces threads with the specified name.
	 */
	public static ThreadFactory single(final String threadName)
	{
		return new ThreadFactory()
		{
			public Thread newThread(Runnable r)
			{
				return new Thread(r, threadName);
			}
		};
	}
	
	/**
	 * @return a {@link ThreadFactory} that produces threads with the specified name, suffixed with a hyphen and a
	 * serial number.
	 */
	public static ThreadFactory serial(final String threadName)
	{
		return new ThreadFactory()
		{
			private final AtomicInteger _serialNo = new AtomicInteger();
			
			public Thread newThread(Runnable r)
			{
				return new Thread(r, threadName + "-" + _serialNo.getAndIncrement());
			}
		};
	}
	
	/**
	 * Uses {@link #serial(String)}.
	 */
	public static ExecutorService newCachedThreadPool(String threadName)
	{
		return Executors.newCachedThreadPool(serial(threadName));
	}
	
	/**
	 * Uses {@link #serial(String)} if {@code nThreads} is > 1, otherwise uses {@link #single(String)}.
	 */
	public static ExecutorService newFixedThreadPool(int nThreads, String threadName)
	{
		return Executors.newFixedThreadPool(nThreads, nThreads == 1 ? single(threadName) : serial(threadName));
	}
	
	/**
	 * Uses {@link #serial(String)}.
	 */
	public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String threadName)
	{
		return Executors.newScheduledThreadPool(corePoolSize, serial(threadName));
	}
	
	/**
	 * Uses {@link #single(String)}.
	 */
	public static ExecutorService newSingleThreadExecutor(String threadName)
	{
		return Executors.newSingleThreadExecutor(single(threadName));
	}
	
	/**
	 * Uses {@link #single(String)}.
	 */
	public static ScheduledExecutorService newSingleThreadScheduledExecutor(String threadName)
	{
		return Executors.newSingleThreadScheduledExecutor(single(threadName));
	}
}
