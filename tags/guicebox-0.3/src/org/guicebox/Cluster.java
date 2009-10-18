package org.guicebox;

import com.google.inject.*;

/**
 * Interface for allowing a GuiceBox-based application to participate in clustering schemes.
 * 
 * @author willhains
 */
@ImplementedBy(Standalone.class) public interface Cluster
{
	/**
	 * Join the cluster. The cluster implementation takes over responsibility for starting/stopping the application, by
	 * calling to the specified application control object. The implementation must tolerate calls to this method when
	 * already joined to or joining the cluster.
	 * 
	 * @param app control for starting/stopping the application.
	 */
	void join(Application app);
	
	/**
	 * Leaves the cluster. The cluster implementation relinquishes responsibility for starting/stopping the application.
	 * This method is called when the application explicitly tells GuiceBox to stop or kill the application, implying
	 * that it has taken over responsibility for the GuiceBox state for now. The implementation must tolerate calls to
	 * this method when already left or leaving the cluster.
	 */
	void leave();
}
