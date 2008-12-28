package org.codeshark.guicebox;

import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * A {@link Formatter} that improves upon the {@link SimpleFormatter} used by default in JDK logging. Unless the log
 * message itself contains line breaks, each message is output in a single line, which is very useful for using text
 * manipulation tools, eg. grep.
 * <p>
 * There are some configurable options which can be specified in the JDK logging properties file. See <a
 * href="http://code.google.com/p/guicebox/wiki/LoggingBridge">GuiceBox documentation</a> for details.
 * 
 * @author willhains
 */
public final class BetterFormatter extends Formatter
{
	// Threadsafe access to date formatter
	private final ThreadLocal<DateFormat> _df;
	
	// Message format options
	private final boolean _sourceLink, _threadName;
	
	public BetterFormatter()
	{
		// Set up the date formatter
		final String dateFormat = _getLoggingProperty("dateFormat", DEFAULT_DATE_FORMAT);
		_df = new ThreadLocal<DateFormat>()
		{
			@Override protected DateFormat initialValue()
			{
				return new SimpleDateFormat(dateFormat);
			}
		};
		
		// Format options
		_sourceLink = Boolean.parseBoolean(_getLoggingProperty("sourceLink", "true"));
		_threadName = Boolean.parseBoolean(_getLoggingProperty("threadName", "true"));
	}
	
	// Gets property values from the JDK logging properties file
	private String _getLoggingProperty(String name, String defaultValue)
	{
		final String propertyName = getClass().getName() + "." + name;
		final String propertyValue = LogManager.getLogManager().getProperty(propertyName);
		if(propertyValue == null || propertyValue.trim().length() == 0) return defaultValue;
		return propertyValue;
	}
	
	@Override public String format(LogRecord record)
	{
		final StringBuilder all = new StringBuilder();
		
		// Date & Time
		all.append(_df.get().format(new Date(record.getMillis()))).append(' ');
		
		// Source
		if(_sourceLink)
		{
			final StackTraceElement[] stack = new Throwable().getStackTrace();
			assert stack != null && stack.length > _STACK_DEPTH : "Cannot infer caller";
			final StackTraceElement caller = stack[_STACK_DEPTH];
			all.append('(').append(caller.getFileName()).append(':').append(caller.getLineNumber()).append(") ");
		}
		
		// Thread
		if(_threadName) all.append('[').append(Thread.currentThread().getName()).append("] ");
		
		// Message
		all.append(record.getMessage());
		
		// Line break
		all.append(_LINE_BREAK);
		
		return all.toString();
	}
	
	// Depth of stack produced just by logging library
	private static final int _STACK_DEPTH = 7;
	
	// Environment-specific line break character
	private static final String _LINE_BREAK = System.getProperty("line.separator");
	
	// Default date format
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
}
