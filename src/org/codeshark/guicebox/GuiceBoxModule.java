package org.codeshark.guicebox;

import com.google.inject.*;

/**
 * @author willhains
 */
final class GuiceBoxModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		requestStaticInjection(Log.class);
	}
}
