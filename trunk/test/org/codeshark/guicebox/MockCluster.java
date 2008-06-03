package org.codeshark.guicebox;

import static org.codeshark.guicebox.Log.*;

/**
 * @author willhains
 */
public final class MockCluster implements Cluster
{
	private volatile Thread _delay;
	
	@Override
	public void join(final Runnable startTrigger, final Runnable stopTrigger)
	{
		if(_delay == null || !_delay.isAlive())
		{
			_delay = new Thread("MockCluster")
			{
				@Override
				public void run()
				{
					try
					{
						log.info("Joining MockCluster...");
						while(true)
						{
							Thread.sleep(2500);
							startTrigger.run();
							log.info("~~became PRIMARY");
							Thread.sleep(2000);
							stopTrigger.run();
							log.info("~~became BACKUP");
						}
					}
					catch(InterruptedException e)
					{
						log.info("MockCluster interrupted");
					}
				}
			};
			_delay.start();
		}
	}
	
	@Override
	public void leave()
	{
		if(_delay != null && _delay.isAlive() && !_delay.isInterrupted())
		{
			log.info("Leaving MockCluster...");
			_delay.interrupt();
		}
	}
}
