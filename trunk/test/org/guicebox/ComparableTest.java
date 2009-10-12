package org.guicebox;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Abstract test suite that verifies a class's conformance to the contract of {@link Comparable}.
 * 
 * @author willhains
 */
@Ignore public abstract class ComparableTest<T extends Comparable<T>> extends GeneralContractTest<T>
{
	/**
	 * @return another instance of the class under test, that is logically greater than the specified instance.
	 */
	protected abstract T createGreater(T o);
	
	/**
	 * @return another instance of the class under test, that is logically less than the specified instance.
	 */
	protected abstract T createLesser(T o);
	
	@Test public void compareTo() throws Exception
	{
		final T o1 = createImp();
		assertEquals(0, o1.compareTo(createEquivalent(o1)));
		assertTrue(o1.compareTo(createLesser(o1)) > 0);
		assertTrue(o1.compareTo(createGreater(o1)) < 0);
	}
	
	private static int _Sgn(final int expr)
	{
		return expr < 0 ? -1 : expr > 0 ? 1 : 0;
	}
	
	private void _signumCheck(final T o1, final T o2)
	{
		assertEquals(_Sgn(o1.compareTo(o2)), -_Sgn(o2.compareTo(o1)));
	}
	
	public void testConsistency() throws Exception
	{
		final T o1 = createImp();
		_signumCheck(o1, createLesser(o1));
		_signumCheck(o1, createEquivalent(o1));
		_signumCheck(o1, createGreater(o1));
	}
	
	@Test @Override public void transitivity() throws Exception
	{
		super.transitivity();
		
		final T o1 = createImp();
		final T o2 = createGreater(o1);
		final T o3 = createGreater(o2);
		assertTrue(o1.compareTo(o3) < 0);
	}
	
	public void testEquivalence() throws Exception
	{
		final T o1 = createImp();
		final T o2 = createEquivalent(o1);
		final T o3 = createGreater(o1);
		assertTrue(o2.compareTo(o3) < 0);
	}
	
	@Override @Test(expected = NullPointerException.class) public void nonNullity() throws Exception
	{
		super.nonNullity();
		createImp().compareTo(null);
	}
	
	@Test @Override public void equalsConsistency() throws Exception
	{
		super.equalsConsistency();
		
		final T o1 = createImp();
		assertTrue(o1.equals(createEquivalent(o1)));
		assertFalse(o1.equals(createLesser(o1)));
	}
}
