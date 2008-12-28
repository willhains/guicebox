package org.codeshark.guicebox.failover;

/**
 * Generic exception indicating that an error occurred while attempting to send/receive a heartbeat.
 * 
 * @author willhains
 */
public class TransportException extends Exception
{
	private static final long serialVersionUID = -827080443455096375L;
	
	/**
	 * Wrap a low-level error that caused the failure to send/receive.
	 */
	public TransportException(Throwable cause)
	{
		super(cause);
	}
	
	/**
	 * Describe a high-level error that caused the failure to send/receive.
	 */
	public TransportException(String message)
	{
		super(message);
	}
}
