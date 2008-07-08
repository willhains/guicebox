package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The {@link LogLevel} from which the {@link ConsoleLogger} will output logs to {@link StdOut} and {@link StdErr}.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface MinLogLevel
{
	
}
