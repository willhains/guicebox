package org.guicebox.failover;

import com.google.inject.*;
import java.net.*;
import java.net.InetAddress;
import java.security.*;
import java.util.*;
import org.guicebox.*;

/**
 * Binds {@link Failover} as the {@link Cluster} implementation.
 * <p>
 * <b>Note:</b> This module sets {@link Security} properties:
 * <ul>
 * <li> {@code networkaddress.cache.ttl = 0}</li>
 * <li> {@code networkaddress.cache.negative.ttl = 0}</li>
 * </ul>
 * 
 * @author willhains
 */
public abstract class FailoverModule extends AbstractModule
{
	private final String _appName;
	
	public FailoverModule(String appName)
	{
		_appName = appName;
	}
	
	@Override protected void configure()
	{
		// Turn off address caching
		Security.setProperty("networkaddress.cache.ttl", "0");
		Security.setProperty("networkaddress.cache.negative.ttl", "0");
		
		// Failover cluster implementation
		bind(Cluster.class).to(Failover.class);
		bind(Ping.class).to(JavaPing.class);
		bind(Heart.class).to(NonBlockingHeart.class);
		
		try
		{
			// Heartbeat constants
			bindConstant().annotatedWith(ApplicationName.class).to(_appName);
			bindConstant().annotatedWith(ProcessId.class).to(UUID.randomUUID().toString());
			bindConstant().annotatedWith(Localhost.class).to(InetAddress.getLocalHost().toString());
		}
		catch(UnknownHostException e)
		{
			throw new RuntimeException("Couldn't identify localhost", e);
		}
	}
}
