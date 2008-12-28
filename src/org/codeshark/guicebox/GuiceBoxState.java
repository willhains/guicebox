package org.codeshark.guicebox;

import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Controls the GuiceBox state machine.
 * 
 * @author willhains
 */
interface GuiceBoxState
{
	Logger _log = Logger.getLogger(GuiceBoxState.class.getName());
	
	GuiceBoxState STOPPED = new GuiceBoxState()
	{
		@Override public GuiceBoxState start(CommandFactory cmdFactory) throws Exception
		{
			// Run start commands
			for(Callable<?> cmd : cmdFactory.getCommands(Start.class))
			{
				cmd.call();
			}
			_log.finer("GuiceBox STARTED");
			return STARTED;
		}
		
		@Override public GuiceBoxState stop(CommandFactory cmdFactory)
		{
			// Already stopped
			return this;
		}
		
		@Override public GuiceBoxState kill(CommandFactory cmdFactory)
		{
			// Run kill commands
			for(Callable<?> cmd : cmdFactory.getCommands(Kill.class))
			{
				try
				{
					cmd.call();
				}
				catch(Exception e)
				{
					// Log and keep going
					_log.log(Level.SEVERE, "Exception while attempting to " + cmd, e);
				}
			}
			_log.info("GuiceBox KILLED");
			return this;
		}
	};
	
	GuiceBoxState STARTED = new GuiceBoxState()
	{
		@Override public GuiceBoxState start(CommandFactory cmdFactory)
		{
			return this;
		}
		
		@Override public GuiceBoxState stop(CommandFactory cmdFactory)
		{
			// Run stop methods
			for(Callable<?> cmd : cmdFactory.getCommands(Stop.class))
			{
				try
				{
					cmd.call();
				}
				catch(Exception e)
				{
					// Log and keep going
					_log.log(Level.SEVERE, "Exception while attempting to " + cmd, e);
				}
			}
			_log.finer("GuiceBox STOPPED");
			return STOPPED;
		}
		
		@Override public GuiceBoxState kill(CommandFactory cmdFactory)
		{
			return stop(cmdFactory).kill(cmdFactory);
		}
	};
	
	GuiceBoxState start(CommandFactory cmdFactory) throws Exception;
	
	GuiceBoxState stop(CommandFactory cmdFactory);
	
	GuiceBoxState kill(CommandFactory cmdFactory);
}