package org.codeshark.guicebox;

import com.google.inject.*;

public class TestMain extends AbstractModule
{
	private TestMain()
	{}
	
	public static void main(String[] args) throws InterruptedException
	{
		// Initialise application
		final GuiceBox guicebox = GuiceBox.init(Guice.createInjector(
			new TestMain(),
			new CommandLineModule(args)));
		
		// Start application
		guicebox.start();
		
		// Wait, then start application again
		Thread.sleep(10000);
		System.out.println("------------------------");
		guicebox.start();
		
		// Wait, then kill application
		Thread.sleep(2000);
		guicebox.kill();
	}
	
	@Override public void configure()
	{
		bind(StartableInterface.class).to(StartableInterfaceImpl.class).in(Scopes.SINGLETON);
		bind(TestStartable.class);
	}
}
