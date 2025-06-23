package com.codebridge.core.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that should be observed for tracing and metrics.
 * This is a custom annotation that will be processed by an aspect to apply
 * observability functionality using Micrometer.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Observed {
    
    /**
     * The name of the observation. This name is used as the metric name and span name.
     * 
     * @return the name of the observation
     */
    String name();
    
    /**
     * The context name for the observation. This is used to group related observations.
     * 
     * @return the context name
     */
    String contextName() default "";
    
    /**
     * Whether to include method parameters in the observation.
     * 
     * @return true if method parameters should be included
     */
    boolean includeParameters() default false;
}

