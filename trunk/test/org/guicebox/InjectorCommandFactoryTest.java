package org.guicebox;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import com.google.inject.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import org.junit.*;

/**
 * @author willhains
 */
public class InjectorCommandFactoryTest
{
	@Test public void good() throws Throwable
	{
		final Injector injector = Guice.createInjector(new GoodModule());
		final CommandFactory cf = new InjectorCommandFactory(injector, Logger.getAnonymousLogger());
		
		final List<String> start = Arrays.asList("StartMethodImpl.startMe()", "Start StartRunnableField.spawnMe");
		for(Callable<?> cmd : cf.getCommands(Start.class))
		{
			assertTrue(start.contains(cmd.toString()));
		}
		final List<String> stop = Arrays.asList("StopKillMethods.stopMe()", "Stop StartRunnableField.spawnMe");
		for(Callable<?> cmd : cf.getCommands(Stop.class))
		{
			assertTrue(stop.contains(cmd.toString()));
		}
		final List<String> kill = Arrays.asList("StopKillMethods.killMe()", "Kill StartRunnableField.spawnMe");
		for(Callable<?> cmd : cf.getCommands(Kill.class))
		{
			assertTrue(kill.contains(cmd.toString()));
		}
	}
	
	@Test(expected = GuiceBoxException.class) public void bad1() throws Throwable
	{
		final Injector injector = Guice.createInjector(new BadModule1());
		new InjectorCommandFactory(injector, Logger.getAnonymousLogger());
	}
	
	@Test(expected = GuiceBoxException.class) public void bad2() throws Throwable
	{
		final Injector injector = Guice.createInjector(new BadModule2());
		new InjectorCommandFactory(injector, Logger.getAnonymousLogger());
	}
}

class GoodModule extends AbstractModule
{
	@Override protected void configure()
	{
		bind(Nothing.class).to(NothingImpl.class);
		bind(StartMethod.class).to(StartMethodImpl.class);
		bind(StopKillMethods.class).to(StopKillMethodsImpl.class);
		bind(StartRunnableField.class);
	}
}

interface Nothing
{
}

class NothingImpl implements Nothing
{
}

interface StartMethod
{
	void startMe();
}

class StartMethodImpl implements StartMethod
{
	@Start public void startMe()
	{
	}
}

interface StopKillMethods
{
	@Stop void stopMe();
	
	@Kill void killMe();
}

class StopKillMethodsImpl implements StopKillMethods
{
	public void stopMe()
	{
	}
	
	public void killMe()
	{
	}
}

class StartRunnableField
{
	@Start final Runnable spawnMe = createNiceMock(Runnable.class);
}

class BadModule1 extends AbstractModule
{
	@Override protected void configure()
	{
		bind(BadGuiceBox1.class);
	}
}

class BadGuiceBox1
{
	@Start final String notRunnable = "I'm not runnable";
}

class BadModule2 extends AbstractModule
{
	@Override protected void configure()
	{
		bind(BadGuiceBox2.class);
	}
}

class BadGuiceBox2
{
	@Start final String notRunnable = "I'm not runnable";
	
	@Stop void stopWithArgs(@SuppressWarnings("unused") String arg)
	{
	}
}
