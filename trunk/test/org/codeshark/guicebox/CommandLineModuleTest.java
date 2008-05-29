package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;

import com.google.inject.*;
import java.lang.annotation.*;
import java.util.*;
import org.junit.*;

public class CommandLineModuleTest
{
	private String[] _split(String args)
	{
		return args.split("\\s");
	}
	
	@Test
	public void testSingleArgument() throws Exception
	{
		final Map<String, String> options = new CommandLineModule(_split("-single TEST"))._constValues;
		assertEquals("TEST", options.get("single"));
	}
	
	@Test
	public void testMultipleArguments() throws Exception
	{
		final Map<String, String> options = new CommandLineModule(_split("-one 1 -two 2 -three 3"))._constValues;
		assertEquals("1", options.get("one"));
		assertEquals("2", options.get("two"));
		assertEquals("3", options.get("three"));
	}
	
	@Test
	public void testNoValueGiven() throws Exception
	{
		final Map<String, String> options = new CommandLineModule(_split("-one 1 -two 2 -three"))._constValues;
		assertEquals("1", options.get("one"));
		assertEquals("2", options.get("two"));
		assertEquals("true", options.get("three"));
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
	
	private static final class Injectable
	{
		@Inject(optional = true)
		@One
		String one = "one";
		
		@Inject(optional = true)
		@Two
		String two = "two";
		
		@Inject(optional = true)
		@Three
		String three = "three";
	}
	
	@Test
	public void testBinding() throws Exception
	{
		final Injector injector = Guice.createInjector(new CommandLineModule(_split( //
		"-org.codeshark.guicebox.CommandLineModuleTest$One 1 "
		    + "-org.codeshark.guicebox.CommandLineModuleTest$Three 3 ")));
		final Injectable injected = injector.getInstance(Injectable.class);
		
		assertEquals("1", injected.one);
		assertEquals("two", injected.two);
		assertEquals("3", injected.three);
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
}
