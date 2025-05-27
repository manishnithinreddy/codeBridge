package com.codebridge.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for securing methods with role-based access control.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredMethod {
    
    /**
     * The roles that are allowed to access the method.
     * If empty, any authenticated user can access the method.
     *
     * @return the allowed roles
     */
    String[] roles() default {};
    
    /**
     * Whether to audit the method access.
     *
     * @return true if the method access should be audited, false otherwise
     */
    boolean audit() default true;
}

