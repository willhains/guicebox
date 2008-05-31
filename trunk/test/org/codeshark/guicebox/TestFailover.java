package org.codeshark.guicebox;

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
		final GuiceBox guicebox = GuiceBox.init(Guice
		    .createInjector(new TestFailover(), new FailoverModule("TestFailover"), new CommandLineModule(args)));
		
		// Start application
		System.out.println("------STARTING GUICEBOX------");
		guicebox.start();
		
		// Wait, then start application again
		Thread.sleep(20000);
		System.out.println("------STARTING GUICEBOX------");
		guicebox.start();
		
		// Wait, then kill application
		Thread.sleep(20000);
		System.out.println("------KILLING GUICEBOX------");
		guicebox.kill();
	}
	
	@Override
	public void configure()
	{
		bind(StartableInterface.class).to(StartableInterfaceImpl.class).in(Scopes.SINGLETON);
		bind(TestStartable.class);
	}
}
