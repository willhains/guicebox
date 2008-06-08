package org.codeshark.guicebox.log4j;

import java.net.*;
import java.util.*;
import org.apache.log4j.*;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.*;
import org.codeshark.guicebox.*;

/**
 * {@link Logger} implementation for popular logging library Log4j. Configures Log4j with the first {@code log4j.xml}
 * file it finds in the classpath (including JARs).
 * 
 * @see "http://logging.apache.org/log4j/1.2/"
 * @author willhains
 */
public final class Log4jLogger implements org.codeshark.guicebox.Logger
{
	// Load config
	static
	{
		final URL config = ClassLoader.getSystemResource("log4j.xml");
		if(config != null && config.getFile().length() != 0) DOMConfigurator.configure(config.getFile());
	}
	
	// Mapping from LogLevels to Levels
	@SuppressWarnings("serial")
	private static final Map<LogLevel, Level> _LEVELS = new EnumMap<LogLevel, Level>(LogLevel.class)
	{
		// Map GuiceBox log levels to Log4j log levels
		{
			put(LogLevel.DEBUG, Level.DEBUG);
			put(LogLevel.INFO, Level.INFO);
			put(LogLevel.WARN, Level.WARN);
			put(LogLevel.ERROR, Level.ERROR);
			put(LogLevel.FATAL, Level.FATAL);
		}
	};
	
	// Bridge to Log4j
	private final Logger _log4jLogger;
	
	/**
	 * Constructs the default logger.
	 */
	private Log4jLogger()
	{
		this(Logger.getRootLogger());
	}
	
	private Log4jLogger(final Logger logger)
	{
		_log4jLogger = logger;
	}
	
	public org.codeshark.guicebox.Logger getNamedLogger(final String name)
	{
		return new Log4jLogger(Logger.getLogger(name));
	}
	
	public void log(LogLevel level, String message)
	{
		_log4jLogger.log(_LEVELS.get(level), message);
	}
	
	public void log(Throwable e)
	{
		_log4jLogger.log(Level.ERROR, e.getClass().getSimpleName(), e);
	}
	
	@Override
	public String toString()
	{
		return _log4jLogger.getName();
	}
	
	@Override
	public boolean isLoggable(LogLevel level)
	{
		return _log4jLogger.isEnabledFor(_LEVELS.get(level));
	}
}
