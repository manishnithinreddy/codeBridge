package com.codebridge.usermanagement.gitlab.controller;

import com.codebridge.usermanagement.gitlab.model.GitLabComment;
import com.codebridge.usermanagement.gitlab.model.GitLabIssue;
import com.codebridge.usermanagement.gitlab.service.GitLabIssueService;
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
 * Controller for GitLab Issue operations.
 */
@RestController
@RequestMapping("/api/gitlab/projects/{projectId}/issues")
@Tag(name = "GitLab Issues", description = "APIs for GitLab Issue operations")
@SecurityRequirement(name = "bearerAuth")
public class GitLabIssueController {

    private final GitLabIssueService gitLabIssueService;

    public GitLabIssueController(GitLabIssueService gitLabIssueService) {
        this.gitLabIssueService = gitLabIssueService;
    }

    /**
     * Get a list of issues for a project.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param state Filter issues by state (opened, closed, all)
     * @param page Page number
     * @param perPage Number of items per page
     * @return List of issues
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get issues",
        description = "Get a list of issues for a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabIssue.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    public ResponseEntity<List<GitLabIssue>> getIssues(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Filter issues by state (opened, closed, all)")
            @RequestParam(required = false, defaultValue = "opened") String state,
            
            @Parameter(description = "Page number")
            @RequestParam(required = false, defaultValue = "1") int page,
            
            @Parameter(description = "Number of items per page")
            @RequestParam(required = false, defaultValue = "20") int perPage) {
        
        List<GitLabIssue> issues = gitLabIssueService.getIssues(
                projectId, token, state, page, perPage);
        
        return ResponseEntity.ok(issues);
    }

    /**
     * Get a specific issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return The issue
     */
    @GetMapping("/{issueIid}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get an issue",
        description = "Get a specific issue by its IID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabIssue.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<GitLabIssue> getIssue(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Issue IID", required = true)
            @PathVariable int issueIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabIssue issue = gitLabIssueService.getIssue(
                projectId, issueIid, token);
        
        if (issue == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(issue);
    }

    /**
     * Create a new issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param requestBody The issue details
     * @return The created issue
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Create an issue",
        description = "Create a new issue in a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabIssue.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    public ResponseEntity<GitLabIssue> createIssue(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Issue details", required = true)
            @RequestBody Map<String, Object> requestBody) {
        
        String title = (String) requestBody.get("title");
        String description = (String) requestBody.get("description");
        String labels = (String) requestBody.get("labels");
        @SuppressWarnings("unchecked")
        List<Integer> assigneeIds = (List<Integer>) requestBody.get("assignee_ids");
        
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        GitLabIssue issue = gitLabIssueService.createIssue(
                projectId, title, description, labels, assigneeIds, token);
        
        if (issue == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(issue);
    }

    /**
     * Update an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @param requestBody The issue details to update
     * @return The updated issue
     */
    @PutMapping("/{issueIid}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Update an issue",
        description = "Update an existing issue in a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabIssue.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<GitLabIssue> updateIssue(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Issue IID", required = true)
            @PathVariable int issueIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Issue details to update", required = true)
            @RequestBody Map<String, Object> requestBody) {
        
        String title = (String) requestBody.get("title");
        String description = (String) requestBody.get("description");
        String state = (String) requestBody.get("state_event");
        String labels = (String) requestBody.get("labels");
        @SuppressWarnings("unchecked")
        List<Integer> assigneeIds = (List<Integer>) requestBody.get("assignee_ids");
        
        GitLabIssue issue = gitLabIssueService.updateIssue(
                projectId, issueIid, title, description, state, labels, assigneeIds, token);
        
        if (issue == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(issue);
    }

    /**
     * Close an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return The updated issue
     */
    @PostMapping("/{issueIid}/close")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Close an issue",
        description = "Close an existing issue in a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabIssue.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<GitLabIssue> closeIssue(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Issue IID", required = true)
            @PathVariable int issueIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabIssue issue = gitLabIssueService.closeIssue(projectId, issueIid, token);
        
        if (issue == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(issue);
    }

    /**
     * Reopen an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return The updated issue
     */
    @PostMapping("/{issueIid}/reopen")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Reopen an issue",
        description = "Reopen a closed issue in a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabIssue.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<GitLabIssue> reopenIssue(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Issue IID", required = true)
            @PathVariable int issueIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabIssue issue = gitLabIssueService.reopenIssue(projectId, issueIid, token);
        
        if (issue == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(issue);
    }

    /**
     * Get comments for an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return List of comments
     */
    @GetMapping("/{issueIid}/comments")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get issue comments",
        description = "Get comments (notes) for an issue",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabComment.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<List<GitLabComment>> getIssueComments(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Issue IID", required = true)
            @PathVariable int issueIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        List<GitLabComment> comments = gitLabIssueService.getIssueComments(
                projectId, issueIid, token);
        
        return ResponseEntity.ok(comments);
    }

    /**
     * Add a comment to an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @param requestBody The comment body
     * @return The created comment
     */
    @PostMapping("/{issueIid}/comments")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Add a comment to an issue",
        description = "Add a new comment to an issue",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabComment.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
        }
    )
    public ResponseEntity<GitLabComment> addIssueComment(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Issue IID", required = true)
            @PathVariable int issueIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Comment body", required = true)
            @RequestBody Map<String, String> requestBody) {
        
        String body = requestBody.get("body");
        
        if (body == null || body.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        GitLabComment comment = gitLabIssueService.addIssueComment(
                projectId, issueIid, body, token);
        
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(comment);
    }
}

