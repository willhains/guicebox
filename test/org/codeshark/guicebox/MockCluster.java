package org.codeshark.guicebox;

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
						System.out.println("Joining MockCluster...");
						while(true)
						{
							Thread.sleep(2500);
							startTrigger.run();
							System.out.println("~~became PRIMARY");
							Thread.sleep(2000);
							stopTrigger.run();
							System.out.println("~~became BACKUP");
						}
					}
					catch(InterruptedException e)
					{
						System.out.println("MockCluster interrupted");
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
			System.out.println("Leaving MockCluster...");
			_delay.interrupt();
		}
	}
}
