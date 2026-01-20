package com.codebridge.featureflag.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the context for evaluating a feature flag.
 */
public class EvaluationContext {
    
    private final String userId;
    private final String sessionId;
    private final Map<String, String> attributes;
    private final Map<String, Double> numericAttributes;
    private final Map<String, Boolean> booleanAttributes;
    private final ServiceContext serviceContext;
    
    private EvaluationContext(Builder builder) {
        this.userId = builder.userId;
        this.sessionId = builder.sessionId;
        this.attributes = builder.attributes;
        this.numericAttributes = builder.numericAttributes;
        this.booleanAttributes = builder.booleanAttributes;
        this.serviceContext = builder.serviceContext;
    }
    
    /**
     * Gets the user ID.
     * 
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the session ID.
     * 
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets the attributes.
     * 
     * @return the attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    /**
     * Gets the numeric attributes.
     * 
     * @return the numeric attributes
     */
    public Map<String, Double> getNumericAttributes() {
        return numericAttributes;
    }
    
    /**
     * Gets the boolean attributes.
     * 
     * @return the boolean attributes
     */
    public Map<String, Boolean> getBooleanAttributes() {
        return booleanAttributes;
    }
    
    /**
     * Gets the service context.
     * 
     * @return the service context
     */
    public ServiceContext getServiceContext() {
        return serviceContext;
    }
    
    /**
     * Creates a new builder.
     * 
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for EvaluationContext.
     */
    public static class Builder {
        
        private String userId;
        private String sessionId;
        private Map<String, String> attributes = new HashMap<>();
        private Map<String, Double> numericAttributes = new HashMap<>();
        private Map<String, Boolean> booleanAttributes = new HashMap<>();
        private ServiceContext serviceContext;
        
        /**
         * Sets the user ID.
         * 
         * @param userId the user ID
         * @return this builder
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        /**
         * Sets the session ID.
         * 
         * @param sessionId the session ID
         * @return this builder
         */
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        /**
         * Sets an attribute.
         * 
         * @param key the attribute key
         * @param value the attribute value
         * @return this builder
         */
        public Builder attribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }
        
        /**
         * Sets attributes.
         * 
         * @param attributes the attributes
         * @return this builder
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }
        
        /**
         * Sets a numeric attribute.
         * 
         * @param key the attribute key
         * @param value the attribute value
         * @return this builder
         */
        public Builder numericAttribute(String key, double value) {
            this.numericAttributes.put(key, value);
            return this;
        }
        
        /**
         * Sets numeric attributes.
         * 
         * @param numericAttributes the numeric attributes
         * @return this builder
         */
        public Builder numericAttributes(Map<String, Double> numericAttributes) {
            this.numericAttributes.putAll(numericAttributes);
            return this;
        }
        
        /**
         * Sets a boolean attribute.
         * 
         * @param key the attribute key
         * @param value the attribute value
         * @return this builder
         */
        public Builder booleanAttribute(String key, boolean value) {
            this.booleanAttributes.put(key, value);
            return this;
        }
        
        /**
         * Sets boolean attributes.
         * 
         * @param booleanAttributes the boolean attributes
         * @return this builder
         */
        public Builder booleanAttributes(Map<String, Boolean> booleanAttributes) {
            this.booleanAttributes.putAll(booleanAttributes);
            return this;
        }
        
        /**
         * Sets the service context.
         * 
         * @param serviceContext the service context
         * @return this builder
         */
        public Builder serviceContext(ServiceContext serviceContext) {
            this.serviceContext = serviceContext;
            return this;
        }
        
        /**
         * Builds the evaluation context.
         * 
         * @return the evaluation context
         */
        public EvaluationContext build() {
            return new EvaluationContext(this);
        }
    }
}

