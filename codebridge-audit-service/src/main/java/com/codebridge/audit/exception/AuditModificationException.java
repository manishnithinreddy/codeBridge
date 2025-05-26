package com.codebridge.audit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to modify an existing audit log.
 * This enforces the immutability of audit logs.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AuditModificationException extends RuntimeException {
    
    /**
     * Creates a new audit modification exception.
     *
     * @param message the error message
     */
    public AuditModificationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new audit modification exception for a specific audit log ID.
     *
     * @param auditId the audit log ID that was attempted to be modified
     * @return a new AuditModificationException with a formatted message
     */
    public static AuditModificationException create(String auditId) {
        return new AuditModificationException(
                String.format("Audit logs are immutable. Cannot modify audit log with ID: '%s'", auditId));
    }
}

