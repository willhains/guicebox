package org.guicebox.failover.udp;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The number of router hops you want the heartbeart to be able to traverse. Talk to your network administrator about
 * what value to use.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface TimeToLive
{	

}
