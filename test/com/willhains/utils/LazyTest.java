package com.willhains.utils;

import static org.junit.Assert.*;
import org.junit.*;

public class LazyTest
{
	@Test
	public void testSingleInit() throws Exception
	{
		final Object o1 = _obj.get();
		final Object o2 = _obj.get();
		assertSame(o1, o2);
	}
	
	private final Lazy<Object> _obj = new Lazy<Object>()
	{
		@Override
		protected Object create()
		{
			return new Object();
		}
	};
	
	@Test
	public void testCircularRef() throws Exception
	{
		try
		{
			_lazyA.get();
			fail("should have thrown IllegalStateException");
		}
		catch(Exception e)
		{
			assertEquals("Circular reference detected", e.getMessage());
		}
	}
	
	private final Lazy<Object> _lazyA = new Lazy<Object>()
	{
		@Override
		protected Object create()
		{
			_lazyB.get();
			return new Object();
		}
	};
	
	private final Lazy<Object> _lazyB = new Lazy<Object>()
	{
		@Override
		protected Object create()
		{
			_lazyA.get();
			return new Object();
		}
	};
}
