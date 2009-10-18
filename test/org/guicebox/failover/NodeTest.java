package org.guicebox.failover;

import static org.junit.Assert.*;

import java.util.*;
import org.guicebox.*;
import org.junit.*;

/**
 * @author willhains
 */
public class NodeTest extends ComparableTest<Node>
{
	private static final Random _RND = new Random();
	
	@Override protected Node createImp() throws Exception
	{
		return new Node("1.1.1.2", Long.toString(_RND.nextLong(), 36));
	}
	
	@Override protected Node createEquivalent(Node o)
	{
		return new Node(o.getAddress(), o.getProcessID());
	}
	
	@Override protected Node createGreater(Node o)
	{
		return new Node("1.1.1.3", o.getProcessID());
	}
	
	@Override protected Node createLesser(Node o)
	{
		return new Node("1.1.1.1", o.getProcessID());
	}
	
	@Test public void testToString()
	{
		assertTrue(imp().toString().contains(imp().getAddress()));
		assertTrue(imp().toString().contains(imp().getProcessID()));
	}
	
	@Test(expected = AssertionError.class) public void nullProcessId()
	{
		new Node("1.1.1.1", null);
	}
	
	@Test(expected = AssertionError.class) public void nullAddress()
	{
		new Node(null, "abc");
	}
	
	@Test public void isSuperiorTo() throws Exception
	{
		assertTrue(imp().isSuperiorTo(createGreater(imp())));
		assertFalse(imp().isSuperiorTo(createEquivalent(imp())));
		assertFalse(imp().isSuperiorTo(createLesser(imp())));
	}
}
