package org.guicebox;

/**
 * The default implementation of {@link Cluster}, implementing a stand-alone application that does not participate in a
 * cluster.
 * 
 * @author willhains
 */
public final class Standalone implements Cluster
{
	public void join(Application app)
	{
		app.start();
	}
	
	public void leave()
	{
		// No action required
	}
	
	public void addListener(ClusterListener listener)
	{
		// No action required
	}
	
	public void removeListener(ClusterListener listener)
	{
		// No action required
	}
}
