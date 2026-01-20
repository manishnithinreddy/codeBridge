package com.codebridge.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Standard error response for API errors.
 */
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
    private String errorCode;

    /**
     * Default constructor.
     */
    public ErrorResponse() {
    }

    /**
     * Constructor with all fields.
     *
     * @param timestamp The timestamp of the error
     * @param status The HTTP status code
     * @param error The error type
     * @param message The error message
     * @param path The request path
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Constructor with status, error, message, path, and timestamp.
     *
     * @param status The HTTP status code
     * @param error The error type
     * @param message The error message
     * @param path The request path
     * @param timestamp The timestamp of the error
     */
    public ErrorResponse(int status, String error, String message, String path, LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }

    /**
     * Constructor with status, error, message, path, timestamp, traceId, and errorCode.
     *
     * @param status The HTTP status code
     * @param error The error type
     * @param message The error message
     * @param path The request path
     * @param timestamp The timestamp of the error
     * @param traceId The trace ID for debugging
     * @param errorCode The error code
     */
    public ErrorResponse(int status, String error, String message, String path, LocalDateTime timestamp, String traceId, String errorCode) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
        this.traceId = traceId;
        this.errorCode = errorCode;
    }

    /**
     * Gets the timestamp of the error.
     *
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the error.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return The status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param status The status code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the error type.
     *
     * @return The error type
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error type.
     *
     * @param error The error type
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the error message.
     *
     * @return The error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message.
     *
     * @param message The error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the request path.
     *
     * @return The request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the request path.
     *
     * @param path The request path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the trace ID.
     *
     * @return The trace ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Sets the trace ID.
     *
     * @param traceId The trace ID
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
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
     * Sets the error code.
     *
     * @param errorCode The error code
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}

