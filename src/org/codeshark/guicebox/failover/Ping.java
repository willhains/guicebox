package org.codeshark.guicebox.failover;

/**
 * Confirms availability of a specified network address.
 * 
 * @author willhains
 */
public interface Ping
{
	/**
	 * Starts pinging the {@link WellKnownAddress} every {@link PingInterval}. Each time a response is received, the
	 * specified listener will get a {@link PingListener#onPing()} callback. If no response is received within
	 * {@link PingInterval} x {@link PingTolerance}, the specified listener will get a
	 * {@link PingListener#onPingTimeout()} callback.
	 */
	void start(final PingListener pingListener);
	
	/**
	 * Temporarily stops pinging the {@link WellKnownAddress}.
	 */
	void stopPinging();
	
	/**
	 * Permanently stops pinging the {@link WellKnownAddress}. The {@link Ping} cannot be used to test connectivity
	 * after calling this method. This method blocks until the internal ping thread is completely shut down, unless the
	 * calling thread is interrupted.
	 */
	void stop();
}