package com.codebridge.common.integration;

/**
 * Exception thrown when an error occurs during integration hook execution.
 */
public class IntegrationException extends RuntimeException {

    private final String errorCode;
    private final String service;

    /**
     * Creates a new integration exception.
     *
     * @param message The error message
     * @param errorCode The error code
     * @param service The service that threw the exception
     */
    public IntegrationException(String message, String errorCode, String service) {
        super(message);
        this.errorCode = errorCode;
        this.service = service;
    }

    /**
     * Creates a new integration exception with a cause.
     *
     * @param message The error message
     * @param cause The cause
     * @param errorCode The error code
     * @param service The service that threw the exception
     */
    public IntegrationException(String message, Throwable cause, String errorCode, String service) {
        super(message, cause);
        this.errorCode = errorCode;
        this.service = service;
    }

    /**
     * Gets the error code.
     *
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the service that threw the exception.
     *
     * @return The service
     */
    public String getService() {
        return service;
    }
}

