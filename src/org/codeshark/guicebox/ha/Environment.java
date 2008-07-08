package org.codeshark.guicebox.ha;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The name of the environment. The unique combination of an {@link ApplicationName} and an {@link Environment} define a
 * cluster.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface Environment
{
	
}
