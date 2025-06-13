package com.codebridge.monitoring.performance.model;

/**
 * Enum representing performance test status.
 */
public enum PerformanceTestStatus {
    /**
     * Test has been created but not run.
     */
    CREATED,
    
    /**
     * Test is currently running.
     */
    RUNNING,
    
    /**
     * Test has completed successfully.
     */
    COMPLETED,
    
    /**
     * Test has failed.
     */
    FAILED,
    
    /**
     * Test has been cancelled.
     */
    CANCELLED
}

