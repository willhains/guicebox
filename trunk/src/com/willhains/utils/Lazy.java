package com.willhains.utils;

public abstract class Lazy<E>
{
	private E _value;

	private boolean _initialising;

	protected abstract E create();

	public final E get()
	{
		if (_initialising) throw new IllegalStateException("Circular reference detected");
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
