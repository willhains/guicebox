package org.codeshark.guicebox;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import com.google.inject.*;
import java.lang.annotation.*;

@Retention(RUNTIME) @Target({ FIELD, PARAMETER }) @BindingAnnotation//
public @interface Param1
{}
