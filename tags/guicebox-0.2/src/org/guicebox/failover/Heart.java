package org.guicebox.failover;

/**
 * Sends and receives heartbeats to advertise this node's presence and detect the presence of other nodes in the
 * cluster.
 * 
 * @author willhains
 */
public interface Heart
{
	/**
	 * Starts listening for heartbeats. Each time a heartbeat is received, the specified listener will get a
	 * {@link HeartbeatListener#onHeartbeat(Heartbeat)} callback. If no response is received within
	 * {@link HeartbeatInterval} x {@link HeartbeatTolerance}, the specified listener will get a
	 * {@link HeartbeatListener#onHeartbeatTimeout()} callback.
	 */
	void listen(final HeartbeatListener heartbeatListener);
	
	/**
	 * Begins sending heartbeats, starting immediately and followed once every {@link HeartbeatInterval} milliseconds.
	 */
	void beat();
	
	/**
	 * Temporarily stops listening for heartbeats.
	 */
	void stopListening();
	
	/**
	 * Temporarily stops sending heartbeats.
	 */
	void stopBeating();
	
	/**
	 * Permanently stops sending/receiving heartbeats. The {@link Heart} cannot be used to send or receive heartbeats
	 * after calling this method. This method blocks until the internal listen & beat threads are completely shut down,
	 * unless the calling thread is interrupted.
	 */
	void stop();
}