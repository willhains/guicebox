package org.codeshark.guicebox;

import com.google.inject.*;

/**
 * Interface for allowing a GuiceBox-based application to participate in clustering schemes.
 * 
 * @author willhains
 */
@ImplementedBy(Standalone.class)
public interface Cluster
{
	/**
	 * Join the cluster. The cluster implementation takes over responsibility for starting/stopping the application, by
	 * using the specified triggers. The implementation must tolerate calls to this method when already joined to or
	 * joining the cluster.
	 * 
	 * @param startTrigger a command to invoke that will start GuiceBox when the cluster deems the node ready.
	 * @param stopTrigger a command to invoke that will stop GuiceBox when the cluster needs the node to sleep.
	 */
	void join(Runnable startTrigger, Runnable stopTrigger);
	
	/**
	 * Leaves the cluster. The cluster implementation relinquishes responsibility for starting/stopping the application.
	 * This method is called when the application explicitly tells GuiceBox to stop or kill the application, implying
	 * that it has taken over responsibility for the GuiceBox state for now. The implementation must tolerate calls to
	 * this method when already left or leaving the cluster.
	 */
	void leave();
}
