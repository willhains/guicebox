package org.codeshark.guicebox;

/**
 * Represents the level of severity of a log message.
 * 
 * @author hainswil
 * @author Last modified by: $Author: graterjo $
 * @version $Revision: 1.19.342.1 $
 */
public enum LogLevel
{
	/**
	 * Log level for debugging purposes.
	 */
	DEBUG,

	/**
	 * Log level for status information.
	 */
	INFO,

	/**
	 * Log level for recoverable error conditions.
	 */
	WARN,

	/**
	 * Log level for non-recoverable error conditions.
	 */
	ERROR,

	/**
	 * Log level for fatal error conditions.
	 */
	FATAL;
}
