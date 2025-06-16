package com.codebridge.featureflag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the context for evaluating a feature flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {
    
    private String userId;
    private String sessionId;
    
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
    
    @Builder.Default
    private Map<String, Double> numericAttributes = new HashMap<>();
    
    @Builder.Default
    private Map<String, Boolean> booleanAttributes = new HashMap<>();
    
    private ServiceContext serviceContext;
    
    /**
     * Converts the evaluation context to a flat map for rule evaluation.
     * 
     * @return a map of attribute names to values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        if (userId != null) {
            map.put("userId", userId);
        }
        
        if (sessionId != null) {
            map.put("sessionId", sessionId);
        }
        
        map.putAll(attributes);
        
        for (Map.Entry<String, Double> entry : numericAttributes.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        
        for (Map.Entry<String, Boolean> entry : booleanAttributes.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        
        if (serviceContext != null) {
            map.put("service.name", serviceContext.getServiceName());
            map.put("service.version", serviceContext.getServiceVersion());
            map.put("service.instanceId", serviceContext.getInstanceId());
            map.put("service.environment", serviceContext.getEnvironment());
            
            if (serviceContext.getMetrics() != null) {
                for (Map.Entry<String, String> entry : serviceContext.getMetrics().entrySet()) {
                    map.put("service.metrics." + entry.getKey(), entry.getValue());
                }
            }
        }
        
        return map;
    }
    
    /**
     * Represents the service context for evaluation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceContext {
        
        private String serviceName;
        private String serviceVersion;
        private String instanceId;
        private String environment;
        
        @Builder.Default
        private Map<String, String> metrics = new HashMap<>();
    }
}

