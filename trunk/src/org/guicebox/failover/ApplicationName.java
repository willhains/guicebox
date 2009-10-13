package org.guicebox.failover;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;
import org.guicebox.*;

/**
 * The name of the application. The unique combination of an {@link ApplicationName} and an {@link UserName} define a
 * cluster.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface ApplicationName
{	

}
