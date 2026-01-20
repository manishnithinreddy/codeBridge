package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.EnvironmentRequest;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.service.EnvironmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.UUID;

/**
 * Controller for environment operations.
 */
@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * Create a new environment.
     *
     * @param request the environment request
     * @param userDetails the authenticated user details
     * @return the created environment
     */
    @PostMapping
    public ResponseEntity<EnvironmentResponse> createEnvironment(
            @Valid @RequestBody EnvironmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        EnvironmentResponse response = environmentService.createEnvironment(request, UUID.fromString(userDetails.getUsername()));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all environments for the authenticated user.
     *
     * @param userDetails the authenticated user details
     * @return list of environments
     */
    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> getAllEnvironments(@AuthenticationPrincipal UserDetails userDetails) {
        List<EnvironmentResponse> environments = environmentService.getAllEnvironments(UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(environments);
    }

    /**
     * Get environment by ID.
     *
     * @param id the environment ID
     * @param userDetails the authenticated user details
     * @return the environment
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> getEnvironmentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        EnvironmentResponse environment = environmentService.getEnvironmentById(id, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(environment);
    }

    /**
     * Update an environment.
     *
     * @param id the environment ID
     * @param request the environment request
     * @param userDetails the authenticated user details
     * @return the updated environment
     */
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> updateEnvironment(
            @PathVariable UUID id,
            @Valid @RequestBody EnvironmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        EnvironmentResponse response = environmentService.updateEnvironment(id, request, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an environment.
     *
     * @param id the environment ID
     * @param userDetails the authenticated user details
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        environmentService.deleteEnvironment(id, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }
}

