package com.codebridge.server.model.enums;

public enum ServerStatus {
    UNKNOWN,
    PROVISIONING, // Added for a more complete lifecycle
    ACTIVE,
    INACTIVE,
    DELETING, // Added for a more complete lifecycle
    ERROR,
    CONNECTION_FAILED // Added for specific connection issues
}
