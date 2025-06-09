package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.EnvironmentRequest;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.service.EnvironmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for project-specific environment operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/environments")
public class ProjectEnvironmentController {

    private final EnvironmentService environmentService;

    public ProjectEnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // For testing purposes, return a fixed UUID
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        return UUID.fromString(authentication.getName());
    }

    /**
     * Create a new environment for a project.
     *
     * @param projectId the project ID
     * @param request the environment request
     * @param authentication the authentication object
     * @return the created environment
     */
    @PostMapping
    public ResponseEntity<EnvironmentResponse> createEnvironment(
            @PathVariable UUID projectId,
            @Valid @RequestBody EnvironmentRequest request,
            Authentication authentication) {
        request.setProjectId(projectId);
        EnvironmentResponse response = environmentService.createEnvironment(request, getUserId(authentication));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all environments for a project.
     *
     * @param projectId the project ID
     * @param authentication the authentication object
     * @return list of environments
     */
    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> getEnvironmentsForProject(
            @PathVariable UUID projectId,
            Authentication authentication) {
        List<EnvironmentResponse> environments = environmentService.getEnvironmentsForProject(
                projectId, getUserId(authentication));
        return ResponseEntity.ok(environments);
    }
}

