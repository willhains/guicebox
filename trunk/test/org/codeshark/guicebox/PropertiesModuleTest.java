package org.codeshark.guicebox;

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
	private ConsoleLogger _log;
	private StringBuilder _out, _err;
	
	@Before
	public void setUp()
	{
		_log = new ConsoleLogger();
		_out = new StringBuilder();
		_err = new StringBuilder();
		_log.setOutStream(new PrintStream(new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				_out.append((char)b);
			}
		}));
		_log.setErrStream(new PrintStream(new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				_err.append((char)b);
			}
		}));
	}
	
	@Test
	public void logHeader()
	{
		final StringBuilder headerText = new StringBuilder();
		headerText.append("**************\n");
		headerText.append("*** HEADER ***\n");
		headerText.append("**************\n");
		
		new PropertiesModule(_log, headerText.toString(), Collections.<Reader> emptyList());
		
		assertEquals(headerText.toString().trim(), _out.toString().trim());
	}
	
	@Test
	public void noLogHeader()
	{
		new PropertiesModule(_log, "", Collections.<Reader> emptyList());
		assertTrue(_out.toString().trim().isEmpty());
	}
	
	@Test
	public void basicProperty()
	{
		final List<Reader> propFiles = Arrays.<Reader> asList(new StringReader("hello=world"));
		assertEquals("world", new PropertiesModule(_log, null, propFiles).getConstant("hello"));
	}
	
	@Test
	public void overrideProperty()
	{
		final PropertiesModule module = new PropertiesModule(_log, "", Arrays.<Reader> asList(
		    new StringReader("prop1=base1\nprop2=base2\nprop3=base3"),
		    new StringReader("prop2=user2"),
		    new StringReader("prop3=user3\nprop4=user4")));
		module.setConstant("prop3", "set3");
		assertEquals("base1", module.getConstant("prop1"));
		assertEquals("user2", module.getConstant("prop2"));
		assertEquals("set3", module.getConstant("prop3"));
		assertEquals("user4", module.getConstant("prop4"));
	}
	
	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER })
	@BindingAnnotation
	@interface BoundInt
	{
	}
	
	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER })
	@interface NonBindingAnnotation
	{
	}
	
	static class ConstantInjected
	{
		@Inject
		@Named("my.property")
		String named;
		
		@Inject
		@BoundInt
		int bound;
	}
	
	@Test
	public void binding()
	{
		final List<Reader> propFiles = Arrays
		    .<Reader> asList(
		        new StringReader("my.property=something"),
		        new StringReader("org.codeshark.guicebox.PropertiesModuleTest$BoundInt=42"),
		        new StringReader("org.codeshark.guicebox.PropertiesModuleTest$NonBindingAnnotation=org.codeshark.guicebox.PropertiesModuleTest$Side.RIGHT"));
		final PropertiesModule module = new PropertiesModule(_log, "", propFiles);
		
		final Injector injector = Guice.createInjector(module);
		final ConstantInjected injected = injector.getInstance(ConstantInjected.class);
		
		assertEquals("something", injected.named);
		assertEquals(42, injected.bound);
		
		assertTrue(_out.toString().contains("    my.property"));
		assertTrue(_out.toString().contains("   @org.codeshark.guicebox.PropertiesModuleTest$BoundInt"));
		assertTrue(_out.toString().contains("    org.codeshark.guicebox.PropertiesModuleTest$NonBindingAnnotation"));
	}
	
	@Test
	public void password()
	{
		final List<Reader> propFiles = Arrays.<Reader> asList(new StringReader("my.password=12345"));
		final PropertiesModule module = new PropertiesModule(_log, "", propFiles);
		
		Guice.createInjector(module);
		
		assertTrue(_out.toString().contains("********"));
	}
	
	@Test
	public void dodgyPropertiesFile()
	{
		final IOException exception = new IOException();
		new PropertiesModule(_log, "", Arrays.<Reader> asList(new Reader()
		{
			@Override
			public void close() throws IOException
			{
			}
			
			@Override
			public int read(char[] cbuf, int off, int len) throws IOException
			{
				throw exception;
			}
		}));
		assertEquals("unable to load properties: " + exception, _err.toString().trim());
	}
}
