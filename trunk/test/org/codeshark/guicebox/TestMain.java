package org.codeshark.guicebox;

import com.google.inject.*;

public class TestMain extends AbstractModule
{
	private TestMain()
	{}
	
	public static void main(String[] args)
	{
		// Configure Guice - wire modules
		GuiceBox.init(Guice.createInjector(new TestMain(), new CommandLineModule(args)));
		
		// Launch application
		GuiceBox.start();
	}
	
	@Override
	public void configure()
	{
		bind(StartableInterface.class).to(StartableInterfaceImpl.class).in(Scopes.SINGLETON);
		bind(TestStartable.class);
	}
}

