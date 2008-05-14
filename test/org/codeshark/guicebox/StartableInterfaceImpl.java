package org.codeshark.guicebox;

import com.google.inject.*;

public class StartableInterfaceImpl implements StartableInterface
{
	private static int _instanceCount;
	private final int _instance;
	
	@Override
    public String toString()
    {
	    return getClass().getSimpleName() + " instance #" + _instance;
    }

	@Inject
	public StartableInterfaceImpl(UnboundStartable unbound)
	{
		_instance = ++_instanceCount;
		System.out.println("Constructed " + this);
	}
	
	@Override
	public void go()
	{
		System.out.println("STARTED " + this);
	}
	
	@Override
	public void og()
	{
		System.out.println("STOPPED " + this);
	}
}
