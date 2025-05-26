package com.codebridge.teams.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error response for validation errors.
 * Extends the standard error response with field-specific error details.
 */
@Getter
@Setter
public class ValidationErrorResponse extends ErrorResponse {
    
    private Map<String, String> errors;
    
    /**
     * Creates a new validation error response.
     *
     * @param status the HTTP status code
     * @param message the error message
     * @param path the request path
     * @param timestamp the error timestamp
     * @param errors the field-specific error details
     */
    public ValidationErrorResponse(int status, String message, String path, 
                                  LocalDateTime timestamp, Map<String, String> errors) {
        super(status, message, path, timestamp);
        this.errors = errors;
    }
}

