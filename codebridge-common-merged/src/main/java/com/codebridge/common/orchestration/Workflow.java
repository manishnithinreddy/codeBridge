package com.codebridge.common.orchestration;

import com.codebridge.common.context.SharedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A workflow that executes a series of steps.
 */
public class Workflow {

    private static final Logger logger = LoggerFactory.getLogger(Workflow.class);

    private final String id;
    private final String name;
    private final String description;
    private final List<WorkflowStep> steps;
    private final Map<String, WorkflowStepResult> results;
    private final SharedContext context;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean aborted;

    /**
     * Creates a new workflow.
     *
     * @param name The name
     * @param description The description
     * @param context The shared context
     */
    public Workflow(String name, String description, SharedContext context) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.steps = new ArrayList<>();
        this.results = new HashMap<>();
        this.context = context;
        this.aborted = false;
    }

    /**
     * Gets the workflow ID.
     *
     * @return The ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the workflow name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the workflow description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the shared context.
     *
     * @return The shared context
     */
    public SharedContext getContext() {
        return context;
    }

    /**
     * Gets the start time.
     *
     * @return The start time, or null if the workflow has not started
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Gets the end time.
     *
     * @return The end time, or null if the workflow has not ended
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Checks if the workflow is aborted.
     *
     * @return True if the workflow is aborted, false otherwise
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * Adds a step to the workflow.
     *
     * @param step The step
     */
    public void addStep(WorkflowStep step) {
        steps.add(step);
    }

    /**
     * Gets all steps in the workflow.
     *
     * @return The steps
     */
    public List<WorkflowStep> getSteps() {
        return new ArrayList<>(steps);
    }

    /**
     * Gets the result for a step.
     *
     * @param stepName The step name
     * @return The result, or null if the step has not been executed
     */
    public WorkflowStepResult getStepResult(String stepName) {
        return results.get(stepName);
    }

    /**
     * Gets all step results.
     *
     * @return The step results
     */
    public Map<String, WorkflowStepResult> getStepResults() {
        return new HashMap<>(results);
    }

    /**
     * Executes the workflow.
     *
     * @return True if the workflow completed successfully, false otherwise
     */
    public boolean execute() {
        startTime = LocalDateTime.now();
        logger.info("Starting workflow: {} ({})", name, id);

        // Sort steps by order
        List<WorkflowStep> sortedSteps = new ArrayList<>(steps);
        sortedSteps.sort(Comparator.comparingInt(WorkflowStep::getOrder));

        boolean success = true;
        for (WorkflowStep step : sortedSteps) {
            if (aborted) {
                logger.info("Workflow aborted: {} ({})", name, id);
                break;
            }

            String stepName = step.getName();
            logger.info("Executing step: {} ({})", stepName, id);

            try {
                if (!step.shouldExecute(context)) {
                    logger.info("Skipping step: {} ({})", stepName, id);
                    results.put(stepName, WorkflowStepResult.skipped("Step skipped"));
                    continue;
                }

                WorkflowStepResult result = step.execute(context);
                results.put(stepName, result);

                if (result.isFailure()) {
                    logger.error("Step failed: {} ({}): {}", stepName, id, result.getMessage());
                    success = false;
                    break;
                }

                logger.info("Step completed: {} ({})", stepName, id);
            } catch (Exception e) {
                logger.error("Error executing step: {} ({})", stepName, id, e);
                WorkflowStepResult result = step.handleError(context, e);
                results.put(stepName, result);
                success = false;
                break;
            }
        }

        endTime = LocalDateTime.now();
        logger.info("Workflow completed: {} ({}), success: {}", name, id, success);
        return success;
    }

    /**
     * Aborts the workflow.
     */
    public void abort() {
        aborted = true;
    }
}

