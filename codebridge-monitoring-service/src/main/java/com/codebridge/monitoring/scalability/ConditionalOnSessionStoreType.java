package com.codebridge.scalability.config;

import com.codebridge.scalability.model.SessionStoreType;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * Annotation for conditional bean creation based on the configured session store type.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(SessionStoreTypeCondition.class)
public @interface ConditionalOnSessionStoreType {
    
    /**
     * The session store type that should be configured for the annotated component to be created.
     *
     * @return the required session store type
     */
    SessionStoreType value();
}

