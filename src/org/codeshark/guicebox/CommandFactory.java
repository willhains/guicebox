package org.codeshark.guicebox;

import com.google.inject.*;
import java.lang.annotation.*;
import java.util.concurrent.*;

/**
 * @author willhains
 */
@ImplementedBy(InjectorCommandFactory.class) interface CommandFactory
{
	/**
	 * Submits all commands for the specified stage to the specified executor.
	 */
	Iterable<Callable<?>> getCommands(Class<? extends Annotation> transition);
}
