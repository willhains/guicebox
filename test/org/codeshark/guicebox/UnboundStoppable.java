package org.codeshark.guicebox;

import static org.codeshark.guicebox.Log.*;

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
		log.info("Constructed", this);
	}
	
	@Stop
	public void stopUnbound()
	{
		log.info("STOPPED", this);
	}
}
