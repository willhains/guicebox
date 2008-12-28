package org.codeshark.guicebox.failover;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The port to broadcast from, used in conjunction with {@link GroupAddress}. Must be different from
 * {@link DestinationPort}. Talk to your network administrator about what value to use.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface SourcePort
{	

}
