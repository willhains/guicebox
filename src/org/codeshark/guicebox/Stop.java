package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;

/**
 * Methods annotated with this will be called by {@link GuiceBox#stop()}.
 * 
 * @author willhains
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface Stop
{}
