package com.codebridge.gitlab.controller;

import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.service.GitLabJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for GitLab CI/CD job operations.
 */
@Slf4j
@RestController
@RequestMapping("/projects/{projectId}/jobs")
@Tag(name = "GitLab Jobs", description = "GitLab CI/CD job operations")
public class GitLabJobController {

    private final GitLabJobService gitLabJobService;

    public GitLabJobController(GitLabJobService gitLabJobService) {
        this.gitLabJobService = gitLabJobService;
    }

    /**
     * Gets all jobs for a project.
     *
     * @param authHeader Authorization header containing GitLab personal access token
     * @param projectId ID of the project
     * @return ResponseEntity containing list of GitLab jobs
     */
    @GetMapping
    @Operation(
        summary = "Get all jobs for a project",
        description = "Retrieves all CI/CD jobs for the specified GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Jobs retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = GitLabJob.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    public ResponseEntity<List<GitLabJob>> getJobs(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "ID of the GitLab project", required = true)
            @PathVariable Long projectId) {
        log.info("Getting jobs for project: {}", projectId);
        
        String accessToken = extractToken(authHeader);
        List<GitLabJob> jobs = gitLabJobService.getJobs(accessToken, projectId);
        
        return ResponseEntity.ok(jobs);
    }

    /**
     * Gets a specific job by ID.
     *
     * @param authHeader Authorization header containing GitLab personal access token
     * @param projectId ID of the project
     * @param jobId ID of the job
     * @return ResponseEntity containing GitLab job
     */
    @GetMapping("/{jobId}")
    @Operation(
        summary = "Get a specific job",
        description = "Retrieves a specific CI/CD job by ID for the specified GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Job retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = GitLabJob.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Job or project not found")
        }
    )
    public ResponseEntity<GitLabJob> getJob(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "ID of the GitLab project", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "ID of the job", required = true)
            @PathVariable Long jobId) {
        log.info("Getting job: {} for project: {}", jobId, projectId);
        
        String accessToken = extractToken(authHeader);
        GitLabJob job = gitLabJobService.getJob(accessToken, projectId, jobId);
        
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(job);
    }

    /**
     * Gets logs for a specific job.
     *
     * @param authHeader Authorization header containing GitLab personal access token
     * @param projectId ID of the project
     * @param jobId ID of the job
     * @return ResponseEntity containing job logs
     */
    @GetMapping("/{jobId}/logs")
    @Operation(
        summary = "Get logs for a job",
        description = "Retrieves the logs for a specific CI/CD job in the specified GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Job logs retrieved successfully",
                content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Job or project not found")
        }
    )
    public ResponseEntity<String> getJobLogs(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "ID of the GitLab project", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "ID of the job", required = true)
            @PathVariable Long jobId) {
        log.info("Getting logs for job: {} in project: {}", jobId, projectId);
        
        String accessToken = extractToken(authHeader);
        String logs = gitLabJobService.getJobLogs(accessToken, projectId, jobId);
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Extracts token from Authorization header.
     *
     * @param authHeader Authorization header
     * @return Extracted token
     */
    private String extractToken(String authHeader) {
        // For Bearer token, extract the token part
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}

