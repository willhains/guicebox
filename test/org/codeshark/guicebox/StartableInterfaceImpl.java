package org.codeshark.guicebox;

import static org.codeshark.guicebox.Log.*;

import com.google.inject.*;

public class StartableInterfaceImpl implements StartableInterface
{
	private static int _instanceCount;
	private final int _instance;
	
	@Override
	public String toString()
	{
		return new StringBuilder(getClass().getSimpleName())
		    .append(" instance #")
		    .append(_instance)
		    .append(" (param1=")
		    .append(param1)
		    .append(", param2=")
		    .append(param2)
		    .append(", param3=")
		    .append(param3)
		    .append(", param4=")
		    .append(param4)
		    .append(")")
		    .toString();
	}
	
	@Inject(optional = true)
	@Param1
	String param1 = "default1";
	
	@Inject(optional = true)
	@Param2
	String param2 = "default2";
	
	@Inject(optional = true)
	@Param3
	String param3 = "default3";
	
	@Inject(optional = true)
	@Param4
	String param4 = "default4";
	
	@Inject
	public StartableInterfaceImpl(UnboundStartable unbound)
	{
		_instance = ++_instanceCount;
		log.info("Constructed", this, "(unbound =", unbound);
	}
	
	@Override
	public void go()
	{
		log.info("STARTED", this);
	}
	
	@Override
	public void og()
	{
		log.info("STOPPED", this);
	}
}
