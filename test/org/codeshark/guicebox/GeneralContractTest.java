package org.codeshark.guicebox;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Abstract test suite that verifies a class's conformance to the general contract of {@code <T>}. Specifically, it
 * tests conformance to the {@link Object#equals(Object)} and {@link Object#hashCode()} contracts.
 * 
 * @param <T> the interface type whose contract is being tested.
 * @author willhains
 */
public abstract class GeneralContractTest<T> extends AbstractTest<T>
{
	/**
	 * @return another instance of the class under test, that is logically equivalent to the specified instance.
	 */
	protected abstract T createEquivalent(T o);
	
	@Test public void equals() throws Exception
	{
		final T o1 = createImp();
		assertEquals(o1, createEquivalent(o1));
	}
	
	@Test public void reflexivity() throws Exception
	{
		final T o1 = createImp();
		assertEquals(o1, o1);
	}
	
	@Test public void symmetry() throws Exception
	{
		final T o1 = createImp();
		final T o2 = createImp();
		assertEquals(o1.equals(o2), o2.equals(o1));
		
		final T o3 = createEquivalent(o2);
		assertEquals(o2.equals(o3), o3.equals(o2));
	}
	
	@Test public void transitivity() throws Exception
	{
		final T o1 = createImp();
		final T o2 = createImp();
		final T o3 = createImp();
		assertEquals(o1.equals(o2) && o2.equals(o3), o1.equals(o3));
		
		final T o4 = createEquivalent(o3);
		final T o5 = createEquivalent(o4);
		assertEquals(o3.equals(o4) && o4.equals(o5), o3.equals(o5));
	}
	
	@Test public void equalsConsistency() throws Exception
	{
		final T o1 = createImp();
		final T o2 = createImp();
		final boolean equal = o1.equals(o2);
		createImp();
		assertEquals(equal, o1.equals(o2));
		
		final T o3 = createEquivalent(o2);
		createImp();
		assertEquals(o2, o3);
	}
	
	@Test public void nonNullity() throws Exception
	{
		final T o1 = createImp();
		assertFalse(o1.equals(null));
	}
	
	@Test public void hashCodeConsistency() throws Exception
	{
		final T o1 = createImp();
		final int hash = o1.hashCode();
		createImp();
		assertEquals(hash, o1.hashCode());
	}
	
	@Test public void equalsHashCodeConsistency() throws Exception
	{
		final T o1 = createImp();
		final T o2 = createImp();
		assertTrue(o1.hashCode() == o2.hashCode() || !o1.equals(o2));
		
		final T o3 = createEquivalent(o2);
		assertEquals(o2.hashCode(), o3.hashCode());
	}
}
