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

	@Inject
	public TestStartable(StartableInterface type1, UnboundStartable type2)
	{
		_instance = ++_instanceCount;
		System.out.println("Constructed " + this);
	}
	
	@Start
	private final Thread _thread = new Thread(toString())
	{
		@Override
        public void run()
        {
	        try
	        {
	        	for(int i = 0; true; i++)
	        	{
	        		Thread.sleep(1000);
	        		System.out.println("    " + getName() + " running...");
	        		if(i > 3) GuiceBox.kill();
	        	}
	        }
	        catch(InterruptedException e)
	        {
	        	System.out.println(getName() + " interrupted. Dying...");
	        }
        }
	};
	
	@Start("Named Thread from Runnable")
	private final Runnable _runnable = new Runnable()
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
