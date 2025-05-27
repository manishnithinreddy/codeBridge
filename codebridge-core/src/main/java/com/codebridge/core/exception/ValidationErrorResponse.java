package com.codebridge.core.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error response for validation errors.
 * Extends the standard error response with field-specific error details.
 */
public class ValidationErrorResponse extends ErrorResponse {
    
    private Map<String, String> errors;
    
    public ValidationErrorResponse() {
    }
    
    public ValidationErrorResponse(int status, String error, String message, String path, 
                                  LocalDateTime timestamp, Map<String, String> errors) {
        super(status, error, message, path, timestamp);
        this.errors = errors;
    }
    
    public ValidationErrorResponse(int status, String error, String message, String path, 
                                  LocalDateTime timestamp, String traceId, String errorCode,
                                  Map<String, String> errors) {
        super(status, error, message, path, timestamp, traceId, errorCode);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}

