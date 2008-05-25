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
		_delay = new Thread("MockCluster")
		{
			@Override
			public void run()
			{
				try
				{
					System.out.println("Joining MockCluster...");
					Thread.sleep(2500);
					System.out.println("~~becoming PRIMARY");
					startTrigger.run();
					System.out.println("~~became PRIMARY");
					Thread.sleep(2000);
					System.out.println("~~becoming BACKUP");
					stopTrigger.run();
					System.out.println("~~became BACKUP");
					Thread.sleep(2500);
					System.out.println("~~becoming PRIMARY");
					startTrigger.run();
					System.out.println("~~became PRIMARY");
					Thread.sleep(3500);
					System.out.println("~~becoming BACKUP");
					stopTrigger.run();
					System.out.println("~~became BACKUP");
				}
				catch(InterruptedException e)
				{
					System.out.println("MockCluster interrupted");
				}
			}
		};
		_delay.start();
	}
	
	@Override
	public void leave()
	{
		System.out.println("Leaving MockCluster...");
		_delay.interrupt();
	}
}
