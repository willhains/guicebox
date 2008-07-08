package org.codeshark.guicebox.ha;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The number of heartbeats it is permissible to miss before declaring ourself primary.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface HeartbeatTolerance
{
	
}
