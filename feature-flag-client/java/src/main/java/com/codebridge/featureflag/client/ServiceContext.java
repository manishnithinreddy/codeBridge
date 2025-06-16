package com.codebridge.featureflag.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the service context for evaluation.
 */
public class ServiceContext {
    
    private final String serviceName;
    private final String serviceVersion;
    private final String instanceId;
    private final String environment;
    private final Map<String, String> metrics;
    
    private ServiceContext(Builder builder) {
        this.serviceName = builder.serviceName;
        this.serviceVersion = builder.serviceVersion;
        this.instanceId = builder.instanceId;
        this.environment = builder.environment;
        this.metrics = builder.metrics;
    }
    
    /**
     * Gets the service name.
     * 
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Gets the service version.
     * 
     * @return the service version
     */
    public String getServiceVersion() {
        return serviceVersion;
    }
    
    /**
     * Gets the instance ID.
     * 
     * @return the instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * Gets the environment.
     * 
     * @return the environment
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Gets the metrics.
     * 
     * @return the metrics
     */
    public Map<String, String> getMetrics() {
        return metrics;
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
     * Builder for ServiceContext.
     */
    public static class Builder {
        
        private String serviceName;
        private String serviceVersion;
        private String instanceId;
        private String environment;
        private Map<String, String> metrics = new HashMap<>();
        
        /**
         * Sets the service name.
         * 
         * @param serviceName the service name
         * @return this builder
         */
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        /**
         * Sets the service version.
         * 
         * @param serviceVersion the service version
         * @return this builder
         */
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }
        
        /**
         * Sets the instance ID.
         * 
         * @param instanceId the instance ID
         * @return this builder
         */
        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }
        
        /**
         * Sets the environment.
         * 
         * @param environment the environment
         * @return this builder
         */
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }
        
        /**
         * Sets a metric.
         * 
         * @param key the metric key
         * @param value the metric value
         * @return this builder
         */
        public Builder metric(String key, String value) {
            this.metrics.put(key, value);
            return this;
        }
        
        /**
         * Sets metrics.
         * 
         * @param metrics the metrics
         * @return this builder
         */
        public Builder metrics(Map<String, String> metrics) {
            this.metrics.putAll(metrics);
            return this;
        }
        
        /**
         * Builds the service context.
         * 
         * @return the service context
         */
        public ServiceContext build() {
            return new ServiceContext(this);
        }
    }
}

