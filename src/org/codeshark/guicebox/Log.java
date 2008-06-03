package org.codeshark.guicebox;

import com.google.inject.*;
import java.text.*;
import java.util.*;

/**
 * A logging bridge to allow applications to easily switch between different logging libraries (for example, at
 * different phases of development). The default logging implementation is {@link ConsoleLogger}. To configure a
 * different implementation, bind a {@link Logger} using Guice.
 * 
 * @see http://code.google.com/p/guicebox/wiki/LoggingBridge
 * @author willhains
 */
public final class Log
{
	// Bridge to implementation
	private final Logger _imp;
	
	@Inject
	private Log(Logger imp)
	{
		_imp = imp;
	}
	
	/**
	 * The default logger.
	 */
	@Inject
	public static Log log = new Log(new ConsoleLogger());
	
	/**
	 * @return a logger for the specified channel name.
	 */
	public static Log forName(String name)
	{
		final Log namedLogger = new Log(log._imp.getNamedLogger(name));
		namedLogger._logTime = log._logTime;
		namedLogger._logThread = log._logThread;
		namedLogger._logSource = log._logSource;
		return namedLogger;
	}
	
	/**
	 * @return a logger for the caller's class name.
	 */
	public static Log forClass()
	{
		final StackTraceElement[] stack = new Throwable().getStackTrace();
		assert stack != null && stack.length > 1 : "Cannot infer caller";
		return forName(stack[1].getClassName());
	}
	
	// Format for prefixing messages with the current time (null = don't include time)
	@Inject(optional = true)
	@LogTime
	private String _logTime;
	
	// Option to prefix messages with a link to the source
	@Inject(optional = true)
	@LogSource
	private boolean _logSource;
	
	// Option to prefix messages with the thread name
	@Inject(optional = true)
	@LogThread
	private boolean _logThread;
	
	// Timestamp formatter
	private final ThreadLocal<DateFormat> _timestamp = new ThreadLocal<DateFormat>()
	{
		@Override
		protected DateFormat initialValue()
		{
			return new SimpleDateFormat(_logTime);
		}
	};
	
	// Builds log message and hands over the bridge
	private void log(LogLevel level, Object... message)
	{
		if(_imp.isLoggable(level))
		{
			final StringBuilder all = new StringBuilder();
			
			// Time
			if(_logTime != null && _logTime.length() > 0) all.append(_timestamp.get().format(new Date())).append(' ');
			
			// Source
			if(_logSource)
			{
				final StackTraceElement[] stack = new Throwable().getStackTrace();
				assert stack != null && stack.length > 2 : "Cannot infer caller";
				final StackTraceElement caller = stack[2];
				all.append('(').append(caller.getFileName()).append(':').append(caller.getLineNumber()).append(") ");
			}
			
			// Thread
			if(_logThread) all.append('[').append(Thread.currentThread().getName()).append("] ");
			
			// Message
			for(int i = 0; i < message.length - 1; i++)
			{
				all.append(message[i]);
				all.append(' ');
			}
			all.append(message[message.length - 1]);
			
			// Call to logging implementation
			_imp.log(level, all.toString());
		}
	}
	
	/**
	 * Logs a message at debug level.
	 */
	public void debug(Object... message)
	{
		log(LogLevel.DEBUG, message);
	}
	
	/**
	 * Logs a message at information level.
	 */
	public void info(Object... message)
	{
		log(LogLevel.INFO, message);
	}
	
	/**
	 * Logs a message at warning level.
	 */
	public void warn(Object... message)
	{
		log(LogLevel.WARN, message);
	}
	
	/**
	 * Logs a message at error level.
	 */
	public void error(Object... message)
	{
		log(LogLevel.ERROR, message);
	}
	
	/**
	 * Logs a message at fatal level.
	 */
	public void fatal(Object... message)
	{
		log(LogLevel.FATAL, message);
	}
	
	/**
	 * Logs an exception.
	 */
	public void exception(Throwable e)
	{
		_imp.log(e);
	}
	
	@Override
	public String toString()
	{
		return _imp.getClass().getSimpleName() + " for " + _imp.toString();
	}
}
