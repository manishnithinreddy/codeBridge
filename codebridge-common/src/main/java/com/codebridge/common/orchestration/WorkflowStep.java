package com.codebridge.common.orchestration;

import com.codebridge.common.context.SharedContext;

/**
 * Interface for workflow steps.
 * Workflow steps are executed as part of a workflow.
 */
public interface WorkflowStep {

    /**
     * Gets the name of the workflow step.
     *
     * @return The name
     */
    String getName();

    /**
     * Gets the description of the workflow step.
     *
     * @return The description
     */
    String getDescription();

    /**
     * Gets the order of the workflow step.
     * Steps are executed in ascending order.
     *
     * @return The order
     */
    int getOrder();

    /**
     * Executes the workflow step.
     *
     * @param context The shared context
     * @return The result of the execution
     * @throws WorkflowException If an error occurs during execution
     */
    WorkflowStepResult execute(SharedContext context) throws WorkflowException;

    /**
     * Checks if the workflow step should be executed.
     * This method is called before the step is executed.
     *
     * @param context The shared context
     * @return True if the step should be executed, false otherwise
     */
    boolean shouldExecute(SharedContext context);

    /**
     * Handles an error that occurred during execution.
     *
     * @param context The shared context
     * @param exception The exception that occurred
     * @return The result of the error handling
     */
    WorkflowStepResult handleError(SharedContext context, Exception exception);
}

