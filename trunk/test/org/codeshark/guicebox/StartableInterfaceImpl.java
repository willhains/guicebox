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
	public StartableInterfaceImpl(
	    UnboundStartable unbound,
	    @Param1 String param1,
	    @Param2 String param2,
	    @Param3 String param3,
	    @Param4 String param4)
	{
		_instance = ++_instanceCount;
		System.out.println("Constructed " + this);
		System.out.println("    param1=" + param1);
		System.out.println("    param2=" + param2);
		System.out.println("    param3=" + param3);
		System.out.println("    param4=" + param4);
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
