package org.guicebox;

import org.junit.*;

/**
 * Superclass for the "Abstract Test Pattern", which is a convenient way to test a class for compliance with an
 * interface's contract.
 * 
 * @param <T> the interface contract being tested.
 * @author willhains
 */
public abstract class AbstractTest<T>
{
	private T _imp;
	
	protected abstract T createImp() throws Exception;
	
	protected final T imp()
	{
		return _imp;
	}
	
	@Before public void setUp() throws Exception
	{
		_imp = createImp();
	}
}
