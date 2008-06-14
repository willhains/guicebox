package org.codeshark.guicebox;

import com.google.inject.*;
import java.io.*;

/**
 * The default {@link Logger} implementation which logs everything to {@code stdout} and {@code stderr}. The {@code
 * stdout} and {@code stderr} streams can be redirected by binding other {@link PrintStream}s to {@link StdOut} and/or
 * {@link StdErr}. The minimum log level can be configured by binding {@link MinLogLevel}.
 * 
 * @author willhains
 */
final class ConsoleLogger implements Logger
{
	ConsoleLogger()
	{
		// Restrict to package access
	}
	
	// Output stream for INFO and below
	private PrintStream _out = System.out;
	
	@Inject(optional = true)
	void setOutStream(@StdOut PrintStream out)
	{
		_out = out;
	}
	
	// Output stream for WARN and above
	private PrintStream _err = System.err;
	
	@Inject(optional = true)
	void setErrStream(@StdErr PrintStream err)
	{
		_err = err;
	}
	
	// Minimum log level to output
	private LogLevel _minLevel = LogLevel.DEBUG;
	
	@Inject(optional = true)
	void setMinLevel(@MinLogLevel LogLevel minLevel)
	{
		_minLevel = minLevel;
	}
	
	/**
	 * This logger does not support channels.
	 * 
	 * @return {@code this}
	 */
	@Override
	public Logger getNamedLogger(String name)
	{
		return this;
	}
	
	/**
	 * Configurable via {@link MinLogLevel}.
	 */
	@Override
	public boolean isLoggable(LogLevel level)
	{
		return _minLevel.compareTo(level) <= 0;
	}
	
	/**
	 * Synchronized to avoid jumbling of stdout and stderr log messages.
	 */
	@Override
	public synchronized void log(LogLevel level, String message)
	{
		switch(level)
		{
			case DEBUG:
			case INFO:
			{
				_out.println(message);
				break;
			}
			case WARN:
			case ERROR:
			case FATAL:
			{
				_err.println(message);
				break;
			}
		}
	}
	
	/**
	 * Synchronized to avoid jumbling of stdout and stderr log messages.
	 */
	@Override
	public synchronized void log(Throwable e)
	{
		e.printStackTrace(_err);
	}
}
