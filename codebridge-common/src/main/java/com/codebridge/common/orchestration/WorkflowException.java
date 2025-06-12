package com.codebridge.common.orchestration;

/**
 * Exception thrown when an error occurs during workflow execution.
 */
public class WorkflowException extends RuntimeException {

    private final String workflowId;
    private final String stepName;

    /**
     * Creates a new workflow exception.
     *
     * @param message The error message
     * @param workflowId The workflow ID
     * @param stepName The step name
     */
    public WorkflowException(String message, String workflowId, String stepName) {
        super(message);
        this.workflowId = workflowId;
        this.stepName = stepName;
    }

    /**
     * Creates a new workflow exception with a cause.
     *
     * @param message The error message
     * @param cause The cause
     * @param workflowId The workflow ID
     * @param stepName The step name
     */
    public WorkflowException(String message, Throwable cause, String workflowId, String stepName) {
        super(message, cause);
        this.workflowId = workflowId;
        this.stepName = stepName;
    }

    /**
     * Gets the workflow ID.
     *
     * @return The workflow ID
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Gets the step name.
     *
     * @return The step name
     */
    public String getStepName() {
        return stepName;
    }
}

