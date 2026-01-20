package com.codebridge.core.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that should be protected by a circuit breaker.
 * This is a custom annotation that will be processed by an aspect to apply
 * circuit breaker functionality using Resilience4j.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    
    /**
     * The name of the circuit breaker. This name is used to identify the circuit breaker
     * configuration in the application properties.
     * 
     * @return the name of the circuit breaker
     */
    String name();
    
    /**
     * The fallback method to call when the circuit breaker is open.
     * The method must be in the same class and have the same signature as the annotated method.
     * 
     * @return the name of the fallback method
     */
    String fallbackMethod() default "";
}

