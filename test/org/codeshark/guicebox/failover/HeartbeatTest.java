package org.codeshark.guicebox.failover;

import static org.junit.Assert.*;

import java.util.*;
import org.codeshark.guicebox.*;
import org.junit.*;

/**
 * @author willhains
 */
public class HeartbeatTest extends GeneralContractTest<Heartbeat>
{
	@Override protected Heartbeat createEquivalent(Heartbeat o)
	{
		return new Heartbeat(o.getAppName(), o.getEnv(), o.getNode());
	}
	
	private static final Random _RND = new Random();
	
	@Override protected Heartbeat createImp() throws Exception
	{
		final String randomIP = _RND.nextInt(256) + "." + _RND.nextInt(256) + "." + _RND.nextInt(256) + "."
			+ _RND.nextInt(256);
		final Node node = new Node(randomIP, Long.toString(_RND.nextLong(), 36));
		return new Heartbeat(Integer.toString(_RND.nextInt(), 36), "TEST", node);
	}
	
	@Test public void isSameCluster()
	{
		// hb1 and hb2 are from the same cluster, but hb2 and hb3 are not
		final Heartbeat hb1 = new Heartbeat("APP1", "PROD", new Node("1.1.1.1", "1a"));
		final Heartbeat hb2 = new Heartbeat("APP1", "PROD", new Node("1.1.1.2", "1b"));
		final Heartbeat hb3 = new Heartbeat("APP1", "DEV", new Node("1.1.1.3", "1a"));
		final Heartbeat hb4 = new Heartbeat("APP2", "PROD", new Node("1.1.1.4", "2a"));
		
		assertTrue(hb1.isSameCluster(hb2));
		assertFalse(hb1.isSameCluster(hb3));
		assertFalse(hb2.isSameCluster(hb3));
		assertFalse(hb1.isSameCluster(hb4));
		assertFalse(hb2.isSameCluster(hb4));
		assertFalse(hb3.isSameCluster(hb4));
		assertFalse(hb1.equals(hb2));
		assertFalse(hb1.toString().equals(hb2));
	}
}
