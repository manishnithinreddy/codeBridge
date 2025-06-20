package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.EnvironmentRequest;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.service.EnvironmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for environment operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * Get all environments for a project.
     *
     * @param projectId the project ID
     * @return list of environment responses
     */
    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> getEnvironments(@PathVariable Long projectId) {
        List<EnvironmentResponse> environments = environmentService.getEnvironments(projectId);
        return ResponseEntity.ok(environments);
    }

    /**
     * Get an environment by ID.
     *
     * @param projectId the project ID
     * @param id the environment ID
     * @return the environment response
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> getEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        EnvironmentResponse environment = environmentService.getEnvironment(id, projectId);
        return ResponseEntity.ok(environment);
    }

    /**
     * Create a new environment.
     *
     * @param projectId the project ID
     * @param request the environment request
     * @return the created environment response
     */
    @PostMapping
    public ResponseEntity<EnvironmentResponse> createEnvironment(
            @PathVariable Long projectId,
            @Valid @RequestBody EnvironmentRequest request) {
        EnvironmentResponse environment = environmentService.createEnvironment(request, projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(environment);
    }

    /**
     * Update an environment.
     *
     * @param projectId the project ID
     * @param id the environment ID
     * @param request the environment request
     * @return the updated environment response
     */
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> updateEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody EnvironmentRequest request) {
        EnvironmentResponse environment = environmentService.updateEnvironment(id, request, projectId);
        return ResponseEntity.ok(environment);
    }

    /**
     * Delete an environment.
     *
     * @param projectId the project ID
     * @param id the environment ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        environmentService.deleteEnvironment(id, projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the default environment for a project.
     *
     * @param projectId the project ID
     * @return the default environment response
     */
    @GetMapping("/default")
    public ResponseEntity<EnvironmentResponse> getDefaultEnvironment(@PathVariable Long projectId) {
        EnvironmentResponse environment = environmentService.getDefaultEnvironment(projectId);
        return ResponseEntity.ok(environment);
    }

    /**
     * Set an environment as the default for a project.
     *
     * @param projectId the project ID
     * @param id the environment ID
     * @return the updated environment response
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<EnvironmentResponse> setDefaultEnvironment(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        EnvironmentResponse environment = environmentService.setDefaultEnvironment(id, projectId);
        return ResponseEntity.ok(environment);
    }
}

