package org.codeshark.guicebox;

import com.google.inject.*;

public final class TestStartable
{
	private static int _instanceCount;
	private final int _instance;
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " instance #" + _instance;
	}
	
	private final GuiceBox _guicebox;
	
	@Inject
	public TestStartable(GuiceBox guicebox, StartableInterface type1, UnboundStartable type2)
	{
		_guicebox = guicebox;
		_instance = ++_instanceCount;
		System.out.println("Constructed " + this);
	}
	
	@Start
	private final Runnable _unnamed = new Runnable()
	{
		@Override
		public void run()
		{
			final String threadName = Thread.currentThread().getName();
			try
			{
				for(int i = 0; true; i++)
				{
					Thread.sleep(1000);
					System.out.println("    " + threadName + " running...");
					if(i > 3)
					{
						_guicebox.stop();
						System.out.println(threadName + " asked GuiceBox to stop");
					}
				}
			}
			catch(InterruptedException e)
			{
				System.out.println(threadName + " interrupted. Dying...");
			}
		}
	};
	
	@Start("Named Thread from TestStartable")
	private final Runnable _named = new Runnable()
	{
		@Override
		public void run()
		{
			final String threadName = Thread.currentThread().getName();
			try
			{
				while(true)
				{
					Thread.sleep(700);
					System.out.println("    " + threadName + " running...");
				}
			}
			catch(InterruptedException e)
			{
				System.out.println(threadName + " interrupted. Dying...");
			}
		}
	};
}
