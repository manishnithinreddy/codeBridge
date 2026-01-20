package com.codebridge.common.orchestration;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of a workflow step execution.
 */
public class WorkflowStepResult {

    /**
     * Status of a workflow step execution.
     */
    public enum Status {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    private final Status status;
    private final String message;
    private final Map<String, Object> data;
    private final Exception exception;

    /**
     * Creates a new workflow step result.
     *
     * @param status The status
     * @param message The message
     * @param data The data
     * @param exception The exception, if any
     */
    private WorkflowStepResult(Status status, String message, Map<String, Object> data, Exception exception) {
        this.status = status;
        this.message = message;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.exception = exception;
    }

    /**
     * Creates a success result.
     *
     * @param message The message
     * @return The result
     */
    public static WorkflowStepResult success(String message) {
        return new WorkflowStepResult(Status.SUCCESS, message, null, null);
    }

    /**
     * Creates a success result with data.
     *
     * @param message The message
     * @param data The data
     * @return The result
     */
    public static WorkflowStepResult success(String message, Map<String, Object> data) {
        return new WorkflowStepResult(Status.SUCCESS, message, data, null);
    }

    /**
     * Creates a failure result.
     *
     * @param message The message
     * @return The result
     */
    public static WorkflowStepResult failure(String message) {
        return new WorkflowStepResult(Status.FAILURE, message, null, null);
    }

    /**
     * Creates a failure result with an exception.
     *
     * @param message The message
     * @param exception The exception
     * @return The result
     */
    public static WorkflowStepResult failure(String message, Exception exception) {
        return new WorkflowStepResult(Status.FAILURE, message, null, exception);
    }

    /**
     * Creates a failure result with data.
     *
     * @param message The message
     * @param data The data
     * @return The result
     */
    public static WorkflowStepResult failure(String message, Map<String, Object> data) {
        return new WorkflowStepResult(Status.FAILURE, message, data, null);
    }

    /**
     * Creates a failure result with data and an exception.
     *
     * @param message The message
     * @param data The data
     * @param exception The exception
     * @return The result
     */
    public static WorkflowStepResult failure(String message, Map<String, Object> data, Exception exception) {
        return new WorkflowStepResult(Status.FAILURE, message, data, exception);
    }

    /**
     * Creates a skipped result.
     *
     * @param message The message
     * @return The result
     */
    public static WorkflowStepResult skipped(String message) {
        return new WorkflowStepResult(Status.SKIPPED, message, null, null);
    }

    /**
     * Gets the status.
     *
     * @return The status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the message.
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the data.
     *
     * @return The data
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    /**
     * Gets a data value.
     *
     * @param key The key
     * @return The value, or null if not found
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * Gets a data value with a specific type.
     *
     * @param key The key
     * @param type The type
     * @param <T> The type
     * @return The value, or null if not found or not of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets the exception.
     *
     * @return The exception, or null if none
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Checks if the result is successful.
     *
     * @return True if the result is successful, false otherwise
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the result is a failure.
     *
     * @return True if the result is a failure, false otherwise
     */
    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    /**
     * Checks if the result is skipped.
     *
     * @return True if the result is skipped, false otherwise
     */
    public boolean isSkipped() {
        return status == Status.SKIPPED;
    }
}

