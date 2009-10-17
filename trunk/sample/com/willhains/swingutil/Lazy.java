package com.willhains.swingutil;

public abstract class Lazy<E>
{
	private E _value;
	
	protected abstract E create();
	
	public final E get()
	{
		return _value == null ? _value = create() : _value;
	}
}
