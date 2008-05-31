package org.codeshark.guicebox.ha;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * How many multiples of {@link PingInterval} to wait before assuming the ping has failed.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@BindingAnnotation
public @interface PingTolerance
{	

}
