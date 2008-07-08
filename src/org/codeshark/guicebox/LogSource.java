package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * Set to {@code true} to prefix log messages with a link to the source code that emitted the log message.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
@BindingAnnotation
public @interface LogSource
{
	
}
