package com.codebridge.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that should be rate limited.
 * This is a custom annotation that will be processed by an aspect to apply
 * rate limiting functionality using Bucket4j.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * The name of the rate limiter. This name is used to identify the rate limiter
     * configuration in the application properties.
     * 
     * @return the name of the rate limiter
     */
    String name();
    
    /**
     * The type of rate limiting to apply.
     * 
     * @return the rate limit type
     */
    RateLimitType type() default RateLimitType.GLOBAL;
    
    /**
     * Enumeration of rate limit types.
     */
    enum RateLimitType {
        /**
         * Global rate limit applied to all requests regardless of user or IP.
         */
        GLOBAL,
        
        /**
         * Rate limit applied per user (requires authentication).
         */
        USER,
        
        /**
         * Rate limit applied per IP address.
         */
        IP
    }
}

