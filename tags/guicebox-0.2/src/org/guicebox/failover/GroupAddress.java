package org.guicebox.failover;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The multicast address for broadcasting "I'm the primary" heartbeat messages to other nodes in the cluster. Must be
 * unique per application and environment. Talk to your network administrator about what value to use.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface GroupAddress
{	

}
