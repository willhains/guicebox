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
		final Node node = imp();
		final Node greater = createGreater(node);
		final Node equiv = createEquivalent(node);
		final Node lesser = createLesser(node);
		
		assertTrue(node.isSuperiorTo(greater));
		assertFalse(node.isSuperiorTo(equiv));
		assertFalse(node.isSuperiorTo(lesser));
		
		assertFalse(greater.isSuperiorTo(lesser));
		assertFalse(greater.isSuperiorTo(equiv));
		assertFalse(equiv.isSuperiorTo(lesser));
		
		assertTrue(lesser.isSuperiorTo(greater));
		assertTrue(lesser.isSuperiorTo(equiv));
		assertTrue(equiv.isSuperiorTo(greater));
	}
	
	@Test public void failDirection()
	{
		final Node node = imp();
		final Node greater = createGreater(node);
		final Node equiv = createEquivalent(node);
		final Node lesser = createLesser(node);
		
		node.setFailDirection(false);
		greater.setFailDirection(false);
		equiv.setFailDirection(false);
		lesser.setFailDirection(false);
		
		assertFalse(node.isSuperiorTo(greater));
		assertTrue(node.isSuperiorTo(equiv));
		assertTrue(node.isSuperiorTo(lesser));
		
		assertTrue(greater.isSuperiorTo(lesser));
		assertTrue(greater.isSuperiorTo(equiv));
		assertTrue(equiv.isSuperiorTo(lesser));
		
		assertFalse(lesser.isSuperiorTo(greater));
		assertFalse(lesser.isSuperiorTo(equiv));
		assertFalse(equiv.isSuperiorTo(greater));
	}
}
