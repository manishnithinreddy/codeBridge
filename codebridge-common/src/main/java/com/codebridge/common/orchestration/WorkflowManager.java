package com.codebridge.common.orchestration;

import com.codebridge.common.context.SharedContext;
import com.codebridge.common.context.SharedContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager for workflows.
 * Provides methods for creating, executing, and managing workflows.
 */
@Component
public class WorkflowManager {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowManager.class);
    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Boolean>> workflowFutures = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final SharedContextManager contextManager;
    private final List<WorkflowStep> workflowSteps;

    /**
     * Creates a new workflow manager.
     *
     * @param contextManager The shared context manager
     * @param workflowSteps The workflow steps
     */
    @Autowired
    public WorkflowManager(SharedContextManager contextManager, List<WorkflowStep> workflowSteps) {
        this.contextManager = contextManager;
        this.workflowSteps = workflowSteps;
    }

    /**
     * Creates a new workflow.
     *
     * @param name The name
     * @param description The description
     * @return The workflow
     */
    public Workflow createWorkflow(String name, String description) {
        SharedContext context = contextManager.createContext();
        Workflow workflow = new Workflow(name, description, context);
        workflows.put(workflow.getId(), workflow);
        return workflow;
    }

    /**
     * Gets a workflow by ID.
     *
     * @param id The workflow ID
     * @return The workflow, or null if not found
     */
    public Workflow getWorkflow(String id) {
        return workflows.get(id);
    }

    /**
     * Executes a workflow asynchronously.
     *
     * @param workflow The workflow
     * @return A future that completes when the workflow is done
     */
    public CompletableFuture<Boolean> executeWorkflow(Workflow workflow) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> workflow.execute(), executorService);
        workflowFutures.put(workflow.getId(), future);
        return future;
    }

    /**
     * Executes a workflow with the specified steps asynchronously.
     *
     * @param name The workflow name
     * @param description The workflow description
     * @param stepNames The names of the steps to include
     * @return A future that completes when the workflow is done
     */
    public CompletableFuture<Boolean> executeWorkflow(String name, String description, List<String> stepNames) {
        Workflow workflow = createWorkflow(name, description);
        
        // Add the specified steps
        for (String stepName : stepNames) {
            workflowSteps.stream()
                    .filter(step -> step.getName().equals(stepName))
                    .findFirst()
                    .ifPresent(workflow::addStep);
        }
        
        return executeWorkflow(workflow);
    }

    /**
     * Aborts a workflow.
     *
     * @param id The workflow ID
     * @return True if the workflow was aborted, false otherwise
     */
    public boolean abortWorkflow(String id) {
        Workflow workflow = getWorkflow(id);
        if (workflow != null) {
            workflow.abort();
            return true;
        }
        return false;
    }

    /**
     * Gets the status of a workflow.
     *
     * @param id The workflow ID
     * @return The status, or null if the workflow is not found
     */
    public Map<String, Object> getWorkflowStatus(String id) {
        Workflow workflow = getWorkflow(id);
        if (workflow == null) {
            return null;
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("id", workflow.getId());
        status.put("name", workflow.getName());
        status.put("description", workflow.getDescription());
        status.put("startTime", workflow.getStartTime());
        status.put("endTime", workflow.getEndTime());
        status.put("aborted", workflow.isAborted());
        status.put("completed", workflow.getEndTime() != null);
        
        CompletableFuture<Boolean> future = workflowFutures.get(id);
        status.put("running", future != null && !future.isDone());
        
        Map<String, Object> stepResults = new HashMap<>();
        workflow.getStepResults().forEach((stepName, result) -> {
            Map<String, Object> stepResult = new HashMap<>();
            stepResult.put("status", result.getStatus().name());
            stepResult.put("message", result.getMessage());
            stepResult.put("data", result.getData());
            stepResults.put(stepName, stepResult);
        });
        status.put("stepResults", stepResults);
        
        return status;
    }
}

