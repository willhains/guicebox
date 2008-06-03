package org.codeshark.guicebox;

import com.google.inject.*;

public final class TestStartable
{
	private static final Log log = Log.forClass();
	
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
		log.info("Constructed", this, "(type1 =", type1, "type2 =", type2);
	}
	
	@Start
	@SuppressWarnings("unused")
	private final Runnable _unnamed = new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				for(int i = 0; true; i++)
				{
					Thread.sleep(1000);
					log.info("running...");
					if(i > 3)
					{
						_guicebox.stop();
						log.info("asked GuiceBox to stop");
					}
				}
			}
			catch(InterruptedException e)
			{
				log.info("interrupted. Dying...");
			}
		}
	};
	
	@Start("Named Thread from TestStartable")
	@SuppressWarnings("unused")
	private final Runnable _named = new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					Thread.sleep(700);
					log.info("running...");
				}
			}
			catch(InterruptedException e)
			{
				log.info("interrupted. Dying...");
			}
		}
	};
}
