package com.codebridge.auth.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    /**
     * Creates a new resource not found exception.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Creates a new resource not found exception for a specific resource type and ID.
     *
     * @param resourceName the resource type name
     * @param fieldName the field name used for identification
     * @param fieldValue the field value that was not found
     * @return a new ResourceNotFoundException with a formatted message
     */
    public static ResourceNotFoundException create(String resourceName, String fieldName, Object fieldValue) {
        return new ResourceNotFoundException(
                String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}

