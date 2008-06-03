package org.codeshark.guicebox;

import com.google.inject.*;

/**
 * @author willhains
 */
@ImplementedBy(ConsoleLogger.class)
public interface Logger
{
	/**
	 * @return an instance of the logging implementation specialised for the specified logging channel name.
	 */
	Logger getNamedLogger(String name);
	
	/**
	 * @return {@code true} if this logger is configured to log at the specified log level.
	 */
	boolean isLoggable(LogLevel level);
	
	/**
	 * Logs a message at the specified level.
	 */
	public void log(LogLevel level, String message);
	
	/**
	 * Logs the details of the specified exception (at a log level determined by the implementation).
	 */
	public void log(Throwable e);
}
