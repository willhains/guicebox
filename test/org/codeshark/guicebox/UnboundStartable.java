package org.codeshark.guicebox;

import com.google.inject.*;

@Singleton public class UnboundStartable
{
	private static int _instanceCount;
	private final int _instance;
	
	@Override public String toString()
	{
		return getClass().getSimpleName() + " instance #" + _instance;
	}
	
	public UnboundStartable()
	{
		_instance = ++_instanceCount;
		System.out.println("Constructed " + this);
	}
	
	@Start public void startUnbound()
	{
		System.out.println("STARTED " + this);
	}
}
