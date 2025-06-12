package com.codebridge.apitest.model.enums;

/**
 * Enum for load test patterns.
 */
public enum LoadPattern {
    /**
     * Constant load with all users active throughout the test.
     */
    CONSTANT,
    
    /**
     * Gradually increasing load with more users added over time.
     */
    RAMP_UP,
    
    /**
     * Step-wise increasing load with users added in batches.
     */
    STEP
}

