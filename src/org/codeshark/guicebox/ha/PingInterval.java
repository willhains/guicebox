package org.codeshark.guicebox.ha;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * Time between between ping attempts. Can be specified in seconds or milliseconds (less than 1000 is assumed to be
 * seconds).
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface PingInterval
{
	
}
