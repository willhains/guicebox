package org.codeshark.guicebox.failover.udp;

import com.google.inject.*;
import org.codeshark.guicebox.failover.*;

/**
 * Binds {@link UdpTransport} as the {@link Transport} implementation.
 * 
 * @author willhains
 * @see FailoverModule
 */
public final class UdpFailoverModule extends FailoverModule
{
	public UdpFailoverModule(String appName)
	{
		super(appName);
	}
	
	/**
	 * Includes a call to {@link FailoverModule#configure(Binder)}.
	 */
	@Override protected void configure()
	{
		super.configure();
		bind(Transport.class).to(UdpTransport.class);
	}
}
