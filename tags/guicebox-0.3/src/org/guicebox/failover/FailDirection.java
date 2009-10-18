package org.guicebox.failover;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * Direction to failover when two or more {@link Node}s volunteer to become primary. The default value, {@code true},
 * means the <i>lower</i> IP address and <i>older</i> process should win.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface FailDirection
{	

}
