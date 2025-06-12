package com.codebridge.server.model;

/**
 * Enum for security event types.
 */
public enum SecurityEventType {
    /**
     * A suspicious login attempt was detected.
     */
    SUSPICIOUS_LOGIN,
    
    /**
     * An unusual command was executed.
     */
    UNUSUAL_COMMAND,
    
    /**
     * Multiple failed login attempts were detected.
     */
    MULTIPLE_FAILED_LOGINS,
    
    /**
     * A login from a new location was detected.
     */
    NEW_LOCATION_LOGIN,
    
    /**
     * A new device was used to access the account.
     */
    NEW_DEVICE_LOGIN,
    
    /**
     * A sensitive operation was performed.
     */
    SENSITIVE_OPERATION,
    
    /**
     * A password change was performed.
     */
    PASSWORD_CHANGE,
    
    /**
     * Account settings were changed.
     */
    ACCOUNT_SETTINGS_CHANGE,
    
    /**
     * A security-related API key was created or modified.
     */
    API_KEY_CHANGE
}

