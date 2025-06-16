package com.codebridge.featureflag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents a rule for evaluating a feature flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagRule implements Serializable {
    
    private String id;
    private String name;
    private String description;
    private int priority;
    private List<Condition> conditions;
    private FlagValue value;
    
    /**
     * Represents a condition for a flag rule.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition implements Serializable {
        
        private String attribute;
        private Operator operator;
        private Object value;
        
        /**
         * Evaluates the condition against the provided context.
         * 
         * @param context the evaluation context
         * @return true if the condition is met, false otherwise
         */
        public boolean evaluate(Map<String, Object> context) {
            if (!context.containsKey(attribute)) {
                return false;
            }
            
            Object contextValue = context.get(attribute);
            
            switch (operator) {
                case EQUALS:
                    return contextValue.equals(value);
                case NOT_EQUALS:
                    return !contextValue.equals(value);
                case GREATER_THAN:
                    if (contextValue instanceof Number && value instanceof Number) {
                        return ((Number) contextValue).doubleValue() > ((Number) value).doubleValue();
                    }
                    return false;
                case LESS_THAN:
                    if (contextValue instanceof Number && value instanceof Number) {
                        return ((Number) contextValue).doubleValue() < ((Number) value).doubleValue();
                    }
                    return false;
                case GREATER_THAN_OR_EQUALS:
                    if (contextValue instanceof Number && value instanceof Number) {
                        return ((Number) contextValue).doubleValue() >= ((Number) value).doubleValue();
                    }
                    return false;
                case LESS_THAN_OR_EQUALS:
                    if (contextValue instanceof Number && value instanceof Number) {
                        return ((Number) contextValue).doubleValue() <= ((Number) value).doubleValue();
                    }
                    return false;
                case CONTAINS:
                    if (contextValue instanceof String && value instanceof String) {
                        return ((String) contextValue).contains((String) value);
                    }
                    return false;
                case NOT_CONTAINS:
                    if (contextValue instanceof String && value instanceof String) {
                        return !((String) contextValue).contains((String) value);
                    }
                    return false;
                case IN:
                    if (value instanceof List) {
                        return ((List<?>) value).contains(contextValue);
                    }
                    return false;
                case NOT_IN:
                    if (value instanceof List) {
                        return !((List<?>) value).contains(contextValue);
                    }
                    return false;
                default:
                    return false;
            }
        }
    }
    
    /**
     * Enum representing the possible operators for conditions.
     */
    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN_OR_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        IN,
        NOT_IN
    }
}

