package sample.empty;

import com.google.inject.*;
import org.guicebox.*;
import org.guicebox.failover.*;
import org.guicebox.failover.udp.*;

/**
 * A simple, empty GuiceBox application that simply starts up, registers with JMX, and participates in a
 * {@link Failover} cluster.
 * 
 * @author willhains
 */
public class EmptyGuiceBox
{
	public static void main(String[] args)
	{
		final Injector injector = Guice.createInjector(new CommandLineModule(), new UdpFailoverModule("sample.empty"));
		final GuiceBox guicebox = injector.getInstance(GuiceBox.class);
		guicebox.start();
	}
}
