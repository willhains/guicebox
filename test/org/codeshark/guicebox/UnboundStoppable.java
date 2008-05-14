package org.codeshark.guicebox;

public class UnboundStoppable
{
	private static int _instanceCount;
	private final int _instance;
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " instance #" + _instance;
	}
	
	public UnboundStoppable()
	{
		_instance = ++_instanceCount;
		System.out.println("Constructed " + this);
	}
	
	@Stop
	public void stopUnbound()
	{
		System.out.println("STOPPED " + this);
	}
}
