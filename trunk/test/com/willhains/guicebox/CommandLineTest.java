package com.willhains.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;

import com.google.inject.*;
import java.lang.annotation.*;
import java.util.*;
import org.junit.*;

public class CommandLineTest
{
	private String[] _split(String args)
	{
		return args.split("\\s");
	}
	
	@Test
	public void testSingleArgument() throws Exception
	{
		final Properties options = new CommandLineModule(_split("-single TEST"))._constValues;
		assertEquals("TEST", options.getProperty("single"));
	}
	
	@Test
	public void testMultipleArguments() throws Exception
	{
		final Properties options = new CommandLineModule(_split("-one 1 -two 2 -three 3"))._constValues;
		assertEquals("1", options.getProperty("one"));
		assertEquals("2", options.getProperty("two"));
		assertEquals("3", options.getProperty("three"));
	}
	
	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER })
	@BindingAnnotation
	public @interface One
	{}
	
	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER })
	@BindingAnnotation
	public @interface Two
	{}
	
	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER })
	@BindingAnnotation
	public @interface Three
	{}
	
	@Test
	public void testDefault() throws Exception
	{
		final Properties options = new CommandLineModule(_split("-one 1 -three 3"))
		    .bind(Two.class, "two", "2")
		    .bind(Three.class, "three", "4")._constValues;
		assertEquals("1", options.getProperty("one"));
		assertEquals("2", options.getProperty("two"));
		assertEquals("3", options.getProperty("three"));
	}
	
	@Test
	public void testBinding() throws Exception
	{
		final CommandLineModule mod = new CommandLineModule(_split("-one 1 -three 3"))
		    .bind(One.class, "one")
		    .bind(Two.class, "two", "2")
		    .bind(Three.class, "three", "4");
		assertEquals("one", mod._bindings.get(One.class));
		assertEquals("two", mod._bindings.get(Two.class));
		assertEquals("three", mod._bindings.get(Three.class));
	}
	
	@Test
	public void testNotSupplied() throws Exception
	{
		try
		{
			Guice.createInjector(new CommandLineModule(_split("-one 1 -two 2"))
			    .bind(One.class, "one")
			    .bind(Two.class, "two", "2")
			    .bind(Three.class, "three"));
			fail("Should have thrown IllegalArgumentException");
		}
		catch(IllegalArgumentException e)
		{
			assertEquals("'-three' must be supplied!", e.getMessage());
		}
	}
	
	@Test
	public void testNonSwitch() throws Exception
	{
		try
		{
			new CommandLineModule(_split("-one 1 2 3"));
			fail("Should have thrown IllegalArgumentException");
		}
		catch(IllegalArgumentException e)
		{
			assertEquals("unknown switch: '2'", e.getMessage());
		}
	}
	
	@Test
	public void testNoValueGiven() throws Exception
	{
		try
		{
			new CommandLineModule(_split("-one 1 -two 2 -three"));
			fail("Should have thrown IllegalArgumentException");
		}
		catch(IllegalArgumentException e)
		{
			assertEquals("no value given for '-three'", e.getMessage());
		}
	}
}
