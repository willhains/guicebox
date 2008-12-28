package org.codeshark.guicebox;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.*;

/**
 * @author willhains
 */
public class TypesTest
{
	interface Intfc1
	{
	}
	
	interface Intfc2 extends Intfc1
	{
	}
	
	class Class1 implements Intfc2
	{
	}
	
	class Class2 extends Class1
	{
	}
	
	@Test public void superInterface()
	{
		final List<Class<?>> supertypes = Types.inheritedBy(Class2.class);
		assertTrue(supertypes.contains(Class1.class));
		assertTrue(supertypes.contains(Intfc2.class));
		assertTrue(supertypes.contains(Intfc1.class));
	}
}
