package com.codebridge.featureflag.client;

/**
 * Represents the result of evaluating a feature flag.
 */
public class EvaluationResult {
    
    private final String flagKey;
    private final FlagValue value;
    private final String variationId;
    private final String ruleId;
    private final EvaluationReason reason;
    
    private EvaluationResult(Builder builder) {
        this.flagKey = builder.flagKey;
        this.value = builder.value;
        this.variationId = builder.variationId;
        this.ruleId = builder.ruleId;
        this.reason = builder.reason;
    }
    
    /**
     * Gets the flag key.
     * 
     * @return the flag key
     */
    public String getFlagKey() {
        return flagKey;
    }
    
    /**
     * Gets the flag value.
     * 
     * @return the flag value
     */
    public FlagValue getValue() {
        return value;
    }
    
    /**
     * Gets the variation ID.
     * 
     * @return the variation ID
     */
    public String getVariationId() {
        return variationId;
    }
    
    /**
     * Gets the rule ID.
     * 
     * @return the rule ID
     */
    public String getRuleId() {
        return ruleId;
    }
    
    /**
     * Gets the evaluation reason.
     * 
     * @return the evaluation reason
     */
    public EvaluationReason getReason() {
        return reason;
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
     * Builder for EvaluationResult.
     */
    public static class Builder {
        
        private String flagKey;
        private FlagValue value;
        private String variationId;
        private String ruleId;
        private EvaluationReason reason;
        
        /**
         * Sets the flag key.
         * 
         * @param flagKey the flag key
         * @return this builder
         */
        public Builder flagKey(String flagKey) {
            this.flagKey = flagKey;
            return this;
        }
        
        /**
         * Sets the flag value.
         * 
         * @param value the flag value
         * @return this builder
         */
        public Builder value(FlagValue value) {
            this.value = value;
            return this;
        }
        
        /**
         * Sets the variation ID.
         * 
         * @param variationId the variation ID
         * @return this builder
         */
        public Builder variationId(String variationId) {
            this.variationId = variationId;
            return this;
        }
        
        /**
         * Sets the rule ID.
         * 
         * @param ruleId the rule ID
         * @return this builder
         */
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }
        
        /**
         * Sets the evaluation reason.
         * 
         * @param reason the evaluation reason
         * @return this builder
         */
        public Builder reason(EvaluationReason reason) {
            this.reason = reason;
            return this;
        }
        
        /**
         * Builds the evaluation result.
         * 
         * @return the evaluation result
         */
        public EvaluationResult build() {
            return new EvaluationResult(this);
        }
    }
    
    /**
     * Represents the reason for the evaluation result.
     */
    public static class EvaluationReason {
        
        private final ReasonType type;
        private final String ruleId;
        private final String description;
        private final String errorMessage;
        
        private EvaluationReason(Builder builder) {
            this.type = builder.type;
            this.ruleId = builder.ruleId;
            this.description = builder.description;
            this.errorMessage = builder.errorMessage;
        }
        
        /**
         * Gets the reason type.
         * 
         * @return the reason type
         */
        public ReasonType getType() {
            return type;
        }
        
        /**
         * Gets the rule ID.
         * 
         * @return the rule ID
         */
        public String getRuleId() {
            return ruleId;
        }
        
        /**
         * Gets the description.
         * 
         * @return the description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the error message.
         * 
         * @return the error message
         */
        public String getErrorMessage() {
            return errorMessage;
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
         * Builder for EvaluationReason.
         */
        public static class Builder {
            
            private ReasonType type;
            private String ruleId;
            private String description;
            private String errorMessage;
            
            /**
             * Sets the reason type.
             * 
             * @param type the reason type
             * @return this builder
             */
            public Builder type(ReasonType type) {
                this.type = type;
                return this;
            }
            
            /**
             * Sets the rule ID.
             * 
             * @param ruleId the rule ID
             * @return this builder
             */
            public Builder ruleId(String ruleId) {
                this.ruleId = ruleId;
                return this;
            }
            
            /**
             * Sets the description.
             * 
             * @param description the description
             * @return this builder
             */
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            /**
             * Sets the error message.
             * 
             * @param errorMessage the error message
             * @return this builder
             */
            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }
            
            /**
             * Builds the evaluation reason.
             * 
             * @return the evaluation reason
             */
            public EvaluationReason build() {
                return new EvaluationReason(this);
            }
        }
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

