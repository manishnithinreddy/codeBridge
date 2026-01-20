package com.codebridge.monitoring.performance.model;

/**
 * Enum representing alert severity levels.
 */
public enum AlertSeverity {
    /**
     * Informational alerts that don't require immediate attention.
     */
    INFO,
    
    /**
     * Warning alerts that may require attention.
     */
    WARNING,
    
    /**
     * Error alerts that require attention.
     */
    ERROR,
    
    /**
     * Critical alerts that require immediate attention.
     */
    CRITICAL
}

