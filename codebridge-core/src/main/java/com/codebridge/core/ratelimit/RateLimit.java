package com.codebridge.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for applying rate limiting to methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * The name of the rate limit.
     */
    String name() default "default";
    
    /**
     * The type of rate limit.
     */
    RateLimitType type() default RateLimitType.IP;
    
    /**
     * Enum for different types of rate limits.
     */
    enum RateLimitType {
        /**
         * Rate limit based on IP address.
         */
        IP,
        
        /**
         * Rate limit based on user ID.
         */
        USER,
        
        /**
         * Rate limit based on API endpoint.
         */
        API,
        
        /**
         * Global rate limit.
         */
        GLOBAL
    }
}

