package org.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;
import org.guicebox.failover.*;

/**
 * The name of the environment. The unique combination of an {@link ApplicationName} and an {@link UserName} define a
 * cluster.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface UserName
{	

}
