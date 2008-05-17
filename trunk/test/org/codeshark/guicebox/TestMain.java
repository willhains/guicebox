package org.codeshark.guicebox;

import com.google.inject.*;

public class TestMain extends AbstractModule
{
	private TestMain()
	{}
	
	public static void main(String[] args) throws InterruptedException
	{
		// Initialise application
		GuiceBox.init(Guice.createInjector(
			new TestMain(),
			new CommandLineModule(args)
			    .bind(Param1.class, "param1", "default1")
			    .bind(Param2.class, "param2", "default2")
			    .bind(Param3.class, "param3", "default3")
			    .bind(Param4.class, "param4", "default4")));
		
		// Start application
		GuiceBox.start();
		
		// Wait, then start application again
		Thread.sleep(10000);
		System.out.println("------------------------");
		GuiceBox.start();
		
		// Wait, then kill application
		Thread.sleep(2000);
		GuiceBox.kill();
	}
	
	@Override
	public void configure()
	{
		bind(StartableInterface.class).to(StartableInterfaceImpl.class).in(Scopes.SINGLETON);
		bind(TestStartable.class);
	}
}
