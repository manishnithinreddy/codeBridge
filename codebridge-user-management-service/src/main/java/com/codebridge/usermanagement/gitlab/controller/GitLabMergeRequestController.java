package com.codebridge.usermanagement.gitlab.controller;

import com.codebridge.usermanagement.gitlab.model.*;
import com.codebridge.usermanagement.gitlab.service.GitLabMergeRequestService;
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
 * Controller for GitLab Merge Request operations.
 */
@RestController
@RequestMapping("/api/gitlab/projects/{projectId}/merge-requests")
@Tag(name = "GitLab Merge Requests", description = "APIs for GitLab Merge Request operations")
@SecurityRequirement(name = "bearerAuth")
public class GitLabMergeRequestController {

    private final GitLabMergeRequestService gitLabMergeRequestService;

    public GitLabMergeRequestController(GitLabMergeRequestService gitLabMergeRequestService) {
        this.gitLabMergeRequestService = gitLabMergeRequestService;
    }

    /**
     * Get a list of merge requests for a project.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param state Filter merge requests by state (opened, closed, locked, merged)
     * @param page Page number
     * @param perPage Number of items per page
     * @return List of merge requests
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get merge requests",
        description = "Get a list of merge requests for a GitLab project",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabMergeRequest.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Project not found")
        }
    )
    public ResponseEntity<List<GitLabMergeRequest>> getMergeRequests(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Filter merge requests by state (opened, closed, locked, merged)")
            @RequestParam(required = false, defaultValue = "opened") String state,
            
            @Parameter(description = "Page number")
            @RequestParam(required = false, defaultValue = "1") int page,
            
            @Parameter(description = "Number of items per page")
            @RequestParam(required = false, defaultValue = "20") int perPage) {
        
        List<GitLabMergeRequest> mergeRequests = gitLabMergeRequestService.getMergeRequests(
                projectId, token, state, page, perPage);
        
        return ResponseEntity.ok(mergeRequests);
    }

    /**
     * Get a specific merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return The merge request
     */
    @GetMapping("/{mergeRequestIid}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get a merge request",
        description = "Get a specific merge request by its IID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabMergeRequest.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<GitLabMergeRequest> getMergeRequest(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabMergeRequest mergeRequest = gitLabMergeRequestService.getMergeRequest(
                projectId, mergeRequestIid, token);
        
        if (mergeRequest == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(mergeRequest);
    }

    /**
     * Get the changes (diff) for a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return The merge request diff
     */
    @GetMapping("/{mergeRequestIid}/changes")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get merge request changes",
        description = "Get the changes (diff) for a merge request",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabMergeRequestDiff.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<GitLabMergeRequestDiff> getMergeRequestDiff(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        GitLabMergeRequestDiff diff = gitLabMergeRequestService.getMergeRequestDiff(
                projectId, mergeRequestIid, token);
        
        if (diff == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(diff);
    }

    /**
     * Get comments (notes) for a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return List of comments
     */
    @GetMapping("/{mergeRequestIid}/comments")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get merge request comments",
        description = "Get comments (notes) for a merge request",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabComment.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<List<GitLabComment>> getMergeRequestComments(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        List<GitLabComment> comments = gitLabMergeRequestService.getMergeRequestComments(
                projectId, mergeRequestIid, token);
        
        return ResponseEntity.ok(comments);
    }

    /**
     * Add a comment to a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @param requestBody The comment body
     * @return The created comment
     */
    @PostMapping("/{mergeRequestIid}/comments")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Add a comment to a merge request",
        description = "Add a new comment to a merge request",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabComment.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<GitLabComment> addMergeRequestComment(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Comment body", required = true)
            @RequestBody Map<String, String> requestBody) {
        
        String body = requestBody.get("body");
        
        if (body == null || body.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        GitLabComment comment = gitLabMergeRequestService.addMergeRequestComment(
                projectId, mergeRequestIid, body, token);
        
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(comment);
    }

    /**
     * Add an inline comment to a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @param requestBody The comment details
     * @return The created comment
     */
    @PostMapping("/{mergeRequestIid}/inline-comments")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Add an inline comment to a merge request",
        description = "Add a new inline comment to a specific line in a merge request",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabComment.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<GitLabComment> addInlineComment(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token,
            
            @Parameter(description = "Inline comment details", required = true)
            @RequestBody Map<String, Object> requestBody) {
        
        String body = (String) requestBody.get("body");
        String path = (String) requestBody.get("path");
        Integer line = (Integer) requestBody.get("line");
        String lineType = (String) requestBody.get("line_type");
        
        if (body == null || body.trim().isEmpty() || path == null || line == null || lineType == null) {
            return ResponseEntity.badRequest().build();
        }
        
        GitLabComment comment = gitLabMergeRequestService.addInlineComment(
                projectId, mergeRequestIid, body, path, line, lineType, token);
        
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(comment);
    }

    /**
     * Get approval rules for a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return The approval rules
     */
    @GetMapping("/{mergeRequestIid}/approval-rules")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get merge request approval rules",
        description = "Get approval rules for a merge request",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = GitLabApprovalRule.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<List<GitLabApprovalRule>> getMergeRequestApprovalRules(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        List<GitLabApprovalRule> approvalRules = gitLabMergeRequestService.getMergeRequestApprovalRules(
                projectId, mergeRequestIid, token);
        
        return ResponseEntity.ok(approvalRules);
    }

    /**
     * Approve a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return Success status
     */
    @PostMapping("/{mergeRequestIid}/approve")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Approve a merge request",
        description = "Approve a merge request",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<Void> approveMergeRequest(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        boolean success = gitLabMergeRequestService.approveMergeRequest(
                projectId, mergeRequestIid, token);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Unapprove a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return Success status
     */
    @PostMapping("/{mergeRequestIid}/unapprove")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Unapprove a merge request",
        description = "Unapprove a previously approved merge request",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Merge request not found")
        }
    )
    public ResponseEntity<Void> unapproveMergeRequest(
            @Parameter(description = "Project ID or URL-encoded path", required = true)
            @PathVariable String projectId,
            
            @Parameter(description = "Merge request IID", required = true)
            @PathVariable int mergeRequestIid,
            
            @Parameter(description = "GitLab personal access token", required = true)
            @RequestHeader("X-GitLab-Token") String token) {
        
        boolean success = gitLabMergeRequestService.unapproveMergeRequest(
                projectId, mergeRequestIid, token);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

