package org.codeshark.guicebox;

/**
 * The default implementation of {@link Cluster}, implementing a stand-alone application that does not participate in a
 * cluster.
 * 
 * @author willhains
 */
public final class Standalone implements Cluster
{
	@Override
	public void join(Runnable startTrigger, Runnable stopTrigger)
	{
		startTrigger.run();
	}
	
	@Override
	public void leave()
	{
		// No action required
	}
}
