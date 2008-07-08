package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;
import java.text.*;

/**
 * Set to a datetime format string to prefix log messages with a link to the source code.
 * 
 * @see SimpleDateFormat
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
@BindingAnnotation
public @interface LogTime
{
	
}
