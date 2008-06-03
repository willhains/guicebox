package org.codeshark.guicebox.log4j;

import com.google.inject.*;
import org.codeshark.guicebox.*;

/**
 * Binds {@link Log4jLogger}.
 * 
 * @author willhains
 */
public class Log4jModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		bind(Logger.class).to(Log4jLogger.class);
	}
}
