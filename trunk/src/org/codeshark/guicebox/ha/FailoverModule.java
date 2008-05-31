package org.codeshark.guicebox.ha;

import com.google.inject.*;
import java.net.*;
import java.util.*;
import org.codeshark.guicebox.*;

/**
 * Binds {@link Failover} as the {@link Cluster} implementation.
 * 
 * @author willhains
 */
public final class FailoverModule extends AbstractModule
{
	private final String _appName;
	
	public FailoverModule(String appName)
	{
		_appName = appName;
	}
	
	@Override
	protected void configure()
	{
		try
		{
			// Failover cluster implementation
			bind(Cluster.class).to(Failover.class);
			
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
