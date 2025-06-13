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
import java.util.UUID;

/**
 * Controller for project token operations.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/tokens")
public class ProjectTokenController {

    private final ProjectTokenService projectTokenService;

    @Autowired
    public ProjectTokenController(ProjectTokenService projectTokenService) {
        this.projectTokenService = projectTokenService;
    }

    /**
     * Create a new project token.
     *
     * @param projectId the project ID
     * @param request the token request
     * @param userDetails the authenticated user
     * @return the created token
     */
    @PostMapping
    public ResponseEntity<ProjectTokenResponse> createToken(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectTokenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectTokenResponse response = projectTokenService.createToken(
                projectId, request, UUID.fromString(userDetails.getUsername()));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all tokens for a project.
     *
     * @param projectId the project ID
     * @param userDetails the authenticated user
     * @return the list of tokens
     */
    @GetMapping
    public ResponseEntity<List<ProjectTokenResponse>> getTokens(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectTokenResponse> tokens = projectTokenService.getTokens(
                projectId, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(tokens);
    }

    /**
     * Get a token by ID.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param userDetails the authenticated user
     * @return the token
     */
    @GetMapping("/{tokenId}")
    public ResponseEntity<ProjectTokenResponse> getToken(
            @PathVariable UUID projectId,
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectTokenResponse token = projectTokenService.getToken(
                projectId, tokenId, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(token);
    }

    /**
     * Update a token.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param request the token request
     * @param userDetails the authenticated user
     * @return the updated token
     */
    @PutMapping("/{tokenId}")
    public ResponseEntity<ProjectTokenResponse> updateToken(
            @PathVariable UUID projectId,
            @PathVariable UUID tokenId,
            @Valid @RequestBody ProjectTokenRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProjectTokenResponse token = projectTokenService.updateToken(
                projectId, tokenId, request, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(token);
    }

    /**
     * Delete a token.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param userDetails the authenticated user
     * @return no content
     */
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> deleteToken(
            @PathVariable UUID projectId,
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectTokenService.deleteToken(
                projectId, tokenId, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }
}

