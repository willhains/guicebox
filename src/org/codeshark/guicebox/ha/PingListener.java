package org.codeshark.guicebox.ha;

/**
 * Listener interface for {@link Ping}.
 * 
 * @author willhains
 */
public interface PingListener
{
	/**
	 * Called when each ping response is received.
	 */
	void onPing();
	
	/**
	 * Called when a ping response has not been received within the configured timeout.
	 */
	void onPingTimeout();
}
