package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;

@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface Start
{}
