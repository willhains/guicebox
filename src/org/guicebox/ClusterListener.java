package org.guicebox;

/**
 * Listener interface for classes who are interested in cluster events.
 * 
 * @author willhains
 */
public interface ClusterListener
{
	void onClusterChange(String newState);
}
