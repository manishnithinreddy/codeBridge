package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ProjectTokenRequest;
import com.codebridge.apitest.dto.ProjectTokenResponse;
import com.codebridge.apitest.service.ProjectTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for project token operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tokens")
public class ProjectTokenController {

    private final ProjectTokenService projectTokenService;

    @Autowired
    public ProjectTokenController(ProjectTokenService projectTokenService) {
        this.projectTokenService = projectTokenService;
    }

    /**
     * Get all tokens for a project.
     *
     * @param projectId the project ID
     * @param userDetails the authenticated user details
     * @return list of token responses
     */
    @GetMapping
    public ResponseEntity<List<ProjectTokenResponse>> getProjectTokens(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        List<ProjectTokenResponse> tokens = projectTokenService.getProjectTokens(projectId, userId);
        return ResponseEntity.ok(tokens);
    }

    /**
     * Create a new project token.
     *
     * @param projectId the project ID
     * @param request the token request
     * @param userDetails the authenticated user details
     * @return the created token response
     */
    @PostMapping
    public ResponseEntity<ProjectTokenResponse> createProjectToken(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectTokenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ProjectTokenResponse token = projectTokenService.createProjectToken(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    /**
     * Revoke a project token.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param userDetails the authenticated user details
     * @return no content response
     */
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> revokeProjectToken(
            @PathVariable Long projectId,
            @PathVariable Long tokenId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        projectTokenService.revokeProjectToken(projectId, tokenId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract user ID from user details.
     *
     * @param userDetails the user details
     * @return the user ID
     */
    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        // Implementation would extract the user ID from the UserDetails object
        // This is a placeholder for the actual implementation
        return 1L;
    }
}

