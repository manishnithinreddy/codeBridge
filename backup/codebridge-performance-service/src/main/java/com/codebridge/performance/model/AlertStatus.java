package com.codebridge.performance.model;

/**
 * Enum representing alert status.
 */
public enum AlertStatus {
    /**
     * Alert is active and unacknowledged.
     */
    ACTIVE,
    
    /**
     * Alert has been acknowledged but not resolved.
     */
    ACKNOWLEDGED,
    
    /**
     * Alert has been resolved.
     */
    RESOLVED
}

