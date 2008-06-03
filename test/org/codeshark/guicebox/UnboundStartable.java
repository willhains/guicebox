package org.codeshark.guicebox;

import static org.codeshark.guicebox.Log.*;

import com.google.inject.*;
import com.google.inject.name.*;

@Singleton
public class UnboundStartable
{
	private static int _instanceCount;
	private final int _instance;
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " instance #" + _instance + " (anotherParam=" + _anotherParam + ")";
	}
	
	private final String _anotherParam;
	
	@Inject
	public UnboundStartable(@Named("non.annotation.property") String anotherParam)
	{
		_instance = ++_instanceCount;
		_anotherParam = anotherParam;
		log.info("Constructed ", this);
	}
	
	@Start
	public void startUnbound()
	{
		log.info("STARTED", this);
	}
}
