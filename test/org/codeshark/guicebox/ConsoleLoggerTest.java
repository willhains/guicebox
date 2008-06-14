package org.codeshark.guicebox;

import static org.codeshark.guicebox.LogLevel.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.*;

public class ConsoleLoggerTest
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
	public void testGetNamedLogger()
	{
		final Logger namedLogger = _log.getNamedLogger("named");
		assertSame(_log, namedLogger);
	}
	
	@Test
	public void testIsLoggable()
	{
		_log.setMinLevel(DEBUG);
		for(LogLevel level : LogLevel.values())
		{
			assertTrue(_log.isLoggable(level));
		}
		
		_log.setMinLevel(FATAL);
		for(LogLevel level : EnumSet.of(DEBUG, INFO, WARN, ERROR))
		{
			assertFalse(_log.isLoggable(level));
		}
	}
	
	@Test
	public void testLogLogLevelString()
	{
		_log.log(DEBUG, "hello");
		assertEquals("hello" + System.getProperty("line.separator"), _out.toString());
		
		_log.log(WARN, "world");
		assertEquals("world" + System.getProperty("line.separator"), _err.toString());
	}
	
	@Test
	public void testLogThrowable()
	{
		_log.log(new IllegalArgumentException("codeshark"));
		assertTrue(_err.toString().startsWith("java.lang.IllegalArgumentException: codeshark"));
		assertTrue(_err.toString().contains("at " + getClass().getName()));
	}
}
