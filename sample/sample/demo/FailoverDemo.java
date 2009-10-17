package sample.demo;

import com.google.inject.*;
import org.guicebox.*;
import org.guicebox.failover.udp.*;

/**
 * Simple GUI to demonstrate the operation of a failover cluster.
 * 
 * @author willhains
 */
public class FailoverDemo
{
	public static void main(String[] args)
	{
		final Injector injector = Guice.createInjector(
			new DemoGUIModule(),
			new CommandLineModule(args),
			new UdpFailoverModule("demo.failover"));
		injector.getInstance(GuiceBox.class);
	}
}
