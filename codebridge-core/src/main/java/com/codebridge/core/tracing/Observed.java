package com.codebridge.core.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for tracing method executions.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Observed {
    
    /**
     * The name of the observation.
     */
    String name() default "";
    
    /**
     * The context name for the observation.
     */
    String contextualName() default "";
}

