package com.codebridge.featureflag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of evaluating a feature flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {
    
    private String flagKey;
    private FlagValue value;
    private String variationId;
    private String ruleId;
    private EvaluationReason reason;
    
    /**
     * Represents the reason for the evaluation result.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationReason {
        
        private ReasonType type;
        private String ruleId;
        private String description;
        private String errorMessage;
    }
    
    /**
     * Enum representing the possible reason types for evaluation results.
     */
    public enum ReasonType {
        DEFAULT,
        RULE_MATCH,
        PREREQUISITE_FAILED,
        ERROR,
        DISABLED,
        FALLTHROUGH,
        TARGET_MATCH
    }
}

