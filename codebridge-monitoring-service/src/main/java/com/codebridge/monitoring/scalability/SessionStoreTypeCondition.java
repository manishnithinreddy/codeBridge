package com.codebridge.monitoring.scalability.config;

import com.codebridge.monitoring.scalability.model.SessionStoreType;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Condition implementation for the ConditionalOnSessionStoreType annotation.
 * Checks if the configured session store type matches the required type.
 */
public class SessionStoreTypeCondition implements Condition {

    private static final String PROPERTY_NAME = "codebridge.scalability.session.store-type";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(
                ConditionalOnSessionStoreType.class.getName());
        
        if (attributes == null) {
            return false;
        }
        
        SessionStoreType requiredType = (SessionStoreType) attributes.get("value");
        Environment env = context.getEnvironment();
        String configuredType = env.getProperty(PROPERTY_NAME);
        
        if (configuredType == null) {
            return false;
        }
        
        return requiredType.name().equalsIgnoreCase(configuredType);
    }
}

