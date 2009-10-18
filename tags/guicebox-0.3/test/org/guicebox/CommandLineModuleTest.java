package org.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;

import com.google.inject.*;
import java.lang.annotation.*;
import org.junit.*;

public class CommandLineModuleTest
{
	private String[] _split(String args)
	{
		return args.split("\\s");
	}
	
	@Test public void testSingleArgument() throws Exception
	{
		final CommandLineModule module = new CommandLineModule(_split("-single TEST"));
		assertEquals("TEST", module.getConstant("single"));
	}
	
	@Test public void testMultipleArguments() throws Exception
	{
		final CommandLineModule module = new CommandLineModule(_split("-one 1 -two 2 -three 3"));
		assertEquals("1", module.getConstant("one"));
		assertEquals("2", module.getConstant("two"));
		assertEquals("3", module.getConstant("three"));
	}
	
	@Test public void testNoValueGiven() throws Exception
	{
		final CommandLineModule module = new CommandLineModule(_split("-one 1 -two 2 -three"));
		assertEquals("1", module.getConstant("one"));
		assertEquals("2", module.getConstant("two"));
		assertEquals("true", module.getConstant("three"));
	}
	
	@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface One
	{}
	
	@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface Two
	{}
	
	@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface Three
	{}
	
	private static final class Injectable
	{
		@Inject(optional = true) @One String one = "one";
		
		@Inject(optional = true) @Two String two = "two";
		
		@Inject(optional = true) @Three String three = "three";
	}
	
	@Test public void testBinding() throws Exception
	{
		final Injector injector = Guice.createInjector(new CommandLineModule(_split( //
		"-org.guicebox.CommandLineModuleTest$One 1 -org.guicebox.CommandLineModuleTest$Three 3 ")));
		final Injectable injected = injector.getInstance(Injectable.class);
		
		assertEquals("1", injected.one);
		assertEquals("two", injected.two);
		assertEquals("3", injected.three);
	}
	
	@Test(expected = IllegalArgumentException.class) public void testNonSwitch() throws Exception
	{
		new CommandLineModule(_split("-one 1 2 3"));
	}
}
