package org.guicebox.failover;

import java.util.concurrent.*;

/**
 * Interface to the physical transport layer for broadcast distribution of {@link Heartbeat}s to other nodes on the
 * network.
 * 
 * @author willhains
 */
public interface Transport
{
	/**
	 * Broadcasts the specified heartbeat to other nodes in the cluster.
	 * 
	 * @throws TransportException if an error occurs while sending (wraps real cause of error).
	 */
	void send(Heartbeat hb) throws TransportException;
	
	/**
	 * Blocks for the specified number of milliseconds, or until a valid {@link Heartbeat} is received from another node
	 * in the same cluster.
	 * 
	 * @param ownHeartbeat a heartbeat from this node, used to determine whether received heartbeats are from other
	 * nodes in the same cluster.
	 * @param timeout how many milliseconds to wait before timing out.
	 * @return the heartbeat that was received.
	 * @throws TransportException if an error occurs while waiting or receiving (wraps real cause of error).
	 * @throws TimeoutException if the specified timeout expires before a heartbeat is received.
	 */
	Heartbeat receive(Heartbeat ownHeartbeat, int timeout) throws TransportException, TimeoutException;
	
	/**
	 * Clean up connections used in the transport of heartbeats. Must tolerate multiple calls. Must swallow any
	 * exceptions raised during disconnection.
	 */
	void disconnect();
	
	/**
	 * @return a short descriptor that can be used in thread names.
	 */
	String toString();
}
