package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.io.*;
import java.lang.annotation.*;

/**
 * The {@link PrintStream} for the {@link ConsoleLogger} to log {@link LogLevel#WARN} and above.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface StdErr
{
	
}
