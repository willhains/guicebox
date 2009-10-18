package org.guicebox;

import java.lang.reflect.*;
import java.util.concurrent.*;

/**
 * Command object to invoke a specified method on a specified object in the future.
 * 
 * @author willhains
 */
final class InvokeMethodCommand implements Callable<Object>
{
	private final Method _method;
	private final Object _instance;
	
	InvokeMethodCommand(Method method, Object o) throws GuiceBoxException
	{
		// Check method for arguments
		if(method.getParameterTypes().length > 0)
		{
			throw new GuiceBoxException("Must have no arguments: " + method);
		}
		
		// Make non-public methods accessible
		method.setAccessible(true);
		
		_method = method;
		_instance = o;
	}
	
	public Object call() throws Exception
	{
		return _method.invoke(_instance);
	}
	
	@Override public String toString()
	{
		return _instance.getClass().getSimpleName() + "." + _method.getName() + "()";
	}
}
