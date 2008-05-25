package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;

/**
 * Methods and {@link Runnable} fields annotated with this will be called by {@link GuiceBox#start()}.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target({ FIELD, METHOD })//
public @interface Start
{
	/**
	 * For annotated {@link Runnable}s, the value will be used as the thread name.
	 */
	String value() default "";
}
