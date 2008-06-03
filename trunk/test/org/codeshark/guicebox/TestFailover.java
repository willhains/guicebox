package org.codeshark.guicebox;

import static org.codeshark.guicebox.Log.*;

import com.google.inject.*;
import org.codeshark.guicebox.ha.*;

public class TestFailover extends AbstractModule
{
	private TestFailover()
	{
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		// Initialise application
		final GuiceBox guicebox = GuiceBox.init(
			new TestFailover(),
			new FailoverModule("TestFailover"),
			new CommandLineModule(args));
		
		// Start application
		log.info("------STARTING GUICEBOX------");
		guicebox.start();
		
		// Wait, then start application again
		Thread.sleep(20000);
		log.info("------STARTING GUICEBOX------");
		guicebox.start();
		
		// Wait, then kill application
		Thread.sleep(20000);
		log.info("------KILLING GUICEBOX------");
		guicebox.kill();
	}
	
	@Override
	public void configure()
	{
		bind(StartableInterface.class).to(StartableInterfaceImpl.class).in(Scopes.SINGLETON);
		bind(TestStartable.class);
	}
}
