package org.guicebox.failover;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import com.google.inject.*;
import java.lang.annotation.*;

/**
 * The comma-or-whitespace-separated hostnames or IP addresses of the machines to ping to test network availability.
 * Talk to your network administrator about what values to use.
 * 
 * @author willhains
 */
@Retention(RUNTIME) @Target( { FIELD, PARAMETER }) @BindingAnnotation public @interface WellKnownAddress
{	

}
