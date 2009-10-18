package sample.empty;

import com.google.inject.*;
import javax.management.*;
import org.guicebox.*;
import org.guicebox.failover.*;
import org.guicebox.failover.udp.*;

/**
 * A simple, empty GuiceBox application that simply starts up, registers with JMX, and participates in a
 * {@link Failover} cluster.
 * 
 * @author willhains
 */
public class EmptyFailover
{
	public static void main(String[] args) throws JMException
	{
		final Injector injector = Guice.createInjector(
			new CommandLineModule(args),
			new UdpFailoverModule("sample.empty"));
		final GuiceBox guicebox = injector.getInstance(GuiceBox.class);
		guicebox.registerJMX();
		guicebox.start();
	}
}
