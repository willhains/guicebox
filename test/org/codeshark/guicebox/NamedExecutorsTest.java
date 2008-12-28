package org.codeshark.guicebox;

import static org.codeshark.guicebox.NamedExecutors.*;
import static org.junit.Assert.*;

import java.util.concurrent.*;
import org.easymock.*;
import org.junit.*;

/**
 * @author willhains
 */
public class NamedExecutorsTest
{
	private final Runnable _runnable = EasyMock.createNiceMock(Runnable.class);
	
	@Test public void singleThreadFactory()
	{
		final ThreadFactory tf = single("single");
		assertEquals("single", tf.newThread(_runnable).getName());
	}
	
	@Test public void serialThreadFactory()
	{
		final ThreadFactory tf = serial("serial");
		assertEquals("serial-0", tf.newThread(_runnable).getName());
		assertEquals("serial-1", tf.newThread(_runnable).getName());
		assertEquals("serial-2", tf.newThread(_runnable).getName());
	}
	
	private final class TName implements Callable<String>
	{
		public String call()
		{
			return Thread.currentThread().getName();
		}
	}
	
	@Test public void fixedSingle() throws Throwable
	{
		final ExecutorService t = newFixedThreadPool(1, "single");
		assertEquals("single", t.submit(new TName()).get());
		assertEquals("single", t.submit(new TName()).get());
	}
	
	@Test public void fixedMulti() throws Throwable
	{
		final ExecutorService t = newFixedThreadPool(3, "multi");
		assertTrue(t.submit(new TName()).get().contains("multi-"));
		assertTrue(t.submit(new TName()).get().contains("multi-"));
		assertTrue(t.submit(new TName()).get().contains("multi-"));
	}
	
	@Test public void cached() throws Throwable
	{
		assertTrue(newCachedThreadPool("cached").submit(new TName()).get().contains("cached-"));
	}
	
	@Test public void scheduledPool() throws Throwable
	{
		assertTrue(newScheduledThreadPool(1, "schedP").submit(new TName()).get().contains("schedP-"));
	}
	
	@Test public void singleThread() throws Throwable
	{
		assertEquals("single", newSingleThreadExecutor("single").submit(new TName()).get());
	}
	
	@Test public void scheduledSingle() throws Throwable
	{
		assertEquals("sched1", newSingleThreadScheduledExecutor("sched1").submit(new TName()).get());
	}
}
