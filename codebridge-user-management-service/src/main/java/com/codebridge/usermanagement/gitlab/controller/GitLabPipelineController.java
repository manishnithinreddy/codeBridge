package com.codebridge.usermanagement.gitlab.controller;

import com.codebridge.usermanagement.gitlab.model.GitLabJob;
import com.codebridge.usermanagement.gitlab.model.GitLabPipeline;
import com.codebridge.usermanagement.gitlab.service.GitLabPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for GitLab Pipeline operations.
 */
@RestController
@RequestMapping("/api/gitlab/projects/{projectId}/pipelines")
@Tag(name = "GitLab Pipelines", description = "APIs for GitLab Pipeline operations")
@SecurityRequirement(name = "bearerAuth")
public class GitLabPipelineController {

    private final GitLabPipelineService gitLabPipelineService;

    public GitLabPipelineController(GitLabPipelineService gitLabPipelineService) {
        this.gitLabPipelineService = gitLabPipelineService;
    }

    /**
     * Get a list of pipelines for a project.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param ref Filter pipelines by ref (branch or tag)
     * @param status Filter pipelines by status (running, pending, success, failed, canceled, skipped)
     * @param page Page number
     * @param perPage Number of items per page
     * @return List of pipelines
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get pipelines",
        description = "Get a list of pipelines for a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabPipeline.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    public ResponseEntity<List<GitLabPipeline>> getPipelines(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Filter pipelines by ref (branch or tag)")
            @RequestParam(required = false) String ref,
            
            @Parameter(description = "Filter pipelines by status (running, pending, success, failed, canceled, skipped)")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Page number")
            @RequestParam(required = false, defaultValue = "1") int page,
            
            @Parameter(description = "Number of items per page")
            @RequestParam(required = false, defaultValue = "20") int perPage) {
        
        List<GitLabPipeline> pipelines = gitLabPipelineService.getPipelines(
                projectId, token, ref, status, page, perPage);
        
        return ResponseEntity.ok(pipelines);
    }

    /**
     * Get a specific pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return The pipeline
     */
    @GetMapping("/{pipelineId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get a pipeline",
        description = "Get a specific pipeline by its ID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabPipeline.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Pipeline not found")
        }
    )
    public ResponseEntity<GitLabPipeline> getPipeline(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Pipeline ID", required = true)
            @PathVariable int pipelineId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabPipeline pipeline = gitLabPipelineService.getPipeline(
                projectId, pipelineId, token);
        
        if (pipeline == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(pipeline);
    }

    /**
     * Get jobs for a pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return List of jobs
     */
    @GetMapping("/{pipelineId}/jobs")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get pipeline jobs",
        description = "Get jobs for a pipeline",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabJob.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Pipeline not found")
        }
    )
    public ResponseEntity<List<GitLabJob>> getPipelineJobs(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Pipeline ID", required = true)
            @PathVariable int pipelineId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        List<GitLabJob> jobs = gitLabPipelineService.getPipelineJobs(
                projectId, pipelineId, token);
        
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get job logs.
     *
     * @param projectId The project ID or URL-encoded path
     * @param jobId The job ID
     * @param token The GitLab personal access token
     * @return The job logs
     */
    @GetMapping("/jobs/{jobId}/logs")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get job logs",
        description = "Get logs for a job",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Job not found")
        }
    )
    public ResponseEntity<String> getJobLogs(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Job ID", required = true)
            @PathVariable int jobId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        String logs = gitLabPipelineService.getJobLogs(projectId, jobId, token);
        
        if (logs == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Create a new pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param requestBody The pipeline details
     * @return The created pipeline
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Create a pipeline",
        description = "Create a new pipeline in a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabPipeline.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    public ResponseEntity<GitLabPipeline> createPipeline(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Pipeline details", required = true)
            @RequestBody Map<String, Object> requestBody) {
        
        String ref = (String) requestBody.get("ref");
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) requestBody.get("variables");
        
        if (ref == null || ref.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        GitLabPipeline pipeline = gitLabPipelineService.createPipeline(
                projectId, ref, variables, token);
        
        if (pipeline == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(pipeline);
    }

    /**
     * Retry a pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return The retried pipeline
     */
    @PostMapping("/{pipelineId}/retry")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Retry a pipeline",
        description = "Retry a failed pipeline",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabPipeline.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Pipeline not found")
        }
    )
    public ResponseEntity<GitLabPipeline> retryPipeline(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Pipeline ID", required = true)
            @PathVariable int pipelineId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabPipeline pipeline = gitLabPipelineService.retryPipeline(
                projectId, pipelineId, token);
        
        if (pipeline == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(pipeline);
    }

    /**
     * Cancel a pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return The canceled pipeline
     */
    @PostMapping("/{pipelineId}/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Cancel a pipeline",
        description = "Cancel a running pipeline",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabPipeline.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Pipeline not found")
        }
    )
    public ResponseEntity<GitLabPipeline> cancelPipeline(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Pipeline ID", required = true)
            @PathVariable int pipelineId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabPipeline pipeline = gitLabPipelineService.cancelPipeline(
                projectId, pipelineId, token);
        
        if (pipeline == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(pipeline);
    }
}

