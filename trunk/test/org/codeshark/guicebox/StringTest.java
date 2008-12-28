package org.codeshark.guicebox;

import java.util.*;

/**
 * Tests {@link ComparableTest} on a proven {@link Comparable} implementation.
 * 
 * @author willhains
 */
public final class StringTest extends ComparableTest<String>
{
	private static final Random _RND = new Random();
	
	@Override protected String createImp() throws Exception
	{
		return Long.toString(_RND.nextLong(), 36);
	}
	
	@Override protected String createEquivalent(final String o)
	{
		return new String(o);
	}
	
	@Override protected String createGreater(final String o)
	{
		return o + "a";
	}
	
	@Override protected String createLesser(final String o)
	{
		return o.substring(0, o.length() - 1);
	}
}
