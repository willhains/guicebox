package sample.demo;

import com.google.inject.*;

/**
 * @author willhains
 */
public final class DemoGUIModule extends AbstractModule
{
	@Override protected void configure()
	{
		bind(FailoverFrame.class);
	}
}
