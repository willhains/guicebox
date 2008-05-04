package com.willhains.utils;

/**
 * Utility for lazy initialisation. Not thread safe.
 * 
 * @param <V> the value type.
 * 
 * @author willhains
 */
public abstract class Lazy<V>
{
	private V _value;
	private boolean _initialising;
	
	protected abstract V create();
	
	public final V get()
	{
		if(_initialising) throw new IllegalStateException("Circular reference detected");
		try
		{
			_initialising = true;
			if(_value == null) _value = create();
		}
		catch(RuntimeException e)
		{
			_value = null;
			throw e;
		}
		finally
		{
			_initialising = false;
		}
		return _value;
	}
}
