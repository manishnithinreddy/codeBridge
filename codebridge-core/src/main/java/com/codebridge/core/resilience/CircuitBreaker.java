package com.codebridge.core.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for applying circuit breakers to methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    
    /**
     * The name of the circuit breaker.
     */
    String name() default "";
    
    /**
     * The fallback method to call when the circuit breaker is open.
     */
    String fallbackMethod() default "";
}

