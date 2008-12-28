package org.codeshark.guicebox.failover.udp;

import static org.junit.Assert.*;

import com.google.inject.*;
import java.lang.annotation.*;
import org.codeshark.guicebox.*;
import org.codeshark.guicebox.failover.*;
import org.junit.*;

/**
 * @author willhains
 */
public class UdpFailoverModuleTest
{
	@Test public void checkBindings() throws Exception
	{
		final String appName = "UdpFailoverModuleTest";
		final Injector injector = Guice.createInjector(new UdpFailoverModule(appName), new AbstractModule()
		{
			@Override protected void configure()
			{
				bindConstant().annotatedWith(Environment.class).to("TEST");
				bindConstant().annotatedWith(WellKnownAddress.class).to("1.1.1.1");
				bindConstant().annotatedWith(GroupAddress.class).to("2.2.2.2");
			}
		});
		assertBinding(injector, Cluster.class, Failover.class);
		assertBinding(injector, Ping.class, JavaPing.class);
		assertBinding(injector, Heart.class, NonBlockingHeart.class);
		assertBinding(injector, Transport.class, UdpTransport.class);
		assertEquals(appName, getConstant(injector, ApplicationName.class));
		assertNotNull(getConstant(injector, ProcessId.class));
		assertNotNull(getConstant(injector, Localhost.class));
	}
	
	static void assertBinding(final Injector injector, final Class<?> intfc, final Class<?> impl)
	{
		assertSame(impl, injector.getInstance(intfc).getClass());
	}
	
	static String getConstant(final Injector injector, final Class<? extends Annotation> annotation)
	{
		return injector.getInstance(Key.get(String.class, annotation));
	}
}
