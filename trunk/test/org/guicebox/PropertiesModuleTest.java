package org.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;

import com.google.inject.*;
import com.google.inject.name.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import org.junit.*;

public class PropertiesModuleTest
{
	private InputStream _stream(String input)
	{
		return new ByteArrayInputStream(input.getBytes());
	}
	
	@Test public void logHeader()
	{
		new PropertiesModule("**** HEADER ****", Collections.<InputStream> emptyList());
	}
	
	@Test public void basicProperty()
	{
		final List<InputStream> propFiles = Arrays.<InputStream> asList(_stream("hello=world"));
		assertEquals("world", new PropertiesModule(null, propFiles).getConstant("hello"));
	}
	
	@Test public void overrideProperty()
	{
		final PropertiesModule module = new PropertiesModule("", Arrays.<InputStream> asList(
			_stream("prop1=base1\nprop2=base2\nprop3=base3"),
			_stream("prop2=user2"),
			_stream("prop3=user3\nprop4=user4")));
		module.setConstant("prop3", "set3");
		assertEquals("base1", module.getConstant("prop1"));
		assertEquals("user2", module.getConstant("prop2"));
		assertEquals("set3", module.getConstant("prop3"));
		assertEquals("user4", module.getConstant("prop4"));
	}
	
	@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation @interface BoundInt
	{}
	
	@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @interface NonBindingAnnotation
	{}
	
	static class ConstantInjected
	{
		@Inject @Named("my.property") String named;
		
		@Inject @BoundInt int bound;
	}
	
	@Test public void binding()
	{
		final List<InputStream> propFiles = Arrays
			.<InputStream> asList(
				_stream("my.property=something"),
				_stream("org.guicebox.PropertiesModuleTest$BoundInt=42"),
				_stream("org.guicebox.PropertiesModuleTest$NonBindingAnnotation=org.guicebox.PropertiesModuleTest$Side.RIGHT"));
		final PropertiesModule module = new PropertiesModule("", propFiles);
		final Injector injector = Guice.createInjector(module);
		final ConstantInjected injected = injector.getInstance(ConstantInjected.class);
		
		assertEquals("something", injected.named);
		assertEquals(42, injected.bound);
	}
	
	@Test public void dodgyPropertiesFile()
	{
		final IOException exception = new IOException();
		new PropertiesModule("", Arrays.<InputStream> asList(new InputStream()
		{
			@Override public void close() throws IOException
			{
			}
			
			@Override public int read() throws IOException
			{
				throw exception;
			}
		}));
	}
}
