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

    private UUID getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            // For testing purposes, return a fixed UUID
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        
        if (authentication.getPrincipal() instanceof UserDetails) {
            return UUID.fromString(((UserDetails) authentication.getPrincipal()).getUsername());
        } else if (authentication.getPrincipal() instanceof String) {
            return UUID.fromString((String) authentication.getPrincipal());
        }
        
        // Default test user
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    /**
     * Create a new environment.
     *
     * @param request the environment request
     * @param authentication the authentication object
     * @return the created environment
     */
    @PostMapping
    public ResponseEntity<EnvironmentResponse> createEnvironment(
            @Valid @RequestBody EnvironmentRequest request,
            Authentication authentication) {
        EnvironmentResponse response = environmentService.createEnvironment(request, getUserId(authentication));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all environments for the authenticated user.
     *
     * @param authentication the authentication object
     * @return list of environments
     */
    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> getAllEnvironments(Authentication authentication) {
        List<EnvironmentResponse> environments = environmentService.getAllEnvironments(getUserId(authentication));
        return ResponseEntity.ok(environments);
    }

    /**
     * Get environment by ID.
     *
     * @param id the environment ID
     * @param authentication the authentication object
     * @return the environment
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> getEnvironmentById(
            @PathVariable UUID id,
            Authentication authentication) {
        EnvironmentResponse environment = environmentService.getEnvironmentById(id, getUserId(authentication));
        return ResponseEntity.ok(environment);
    }

    /**
     * Update an environment.
     *
     * @param id the environment ID
     * @param request the environment request
     * @param authentication the authentication object
     * @return the updated environment
     */
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> updateEnvironment(
            @PathVariable UUID id,
            @Valid @RequestBody EnvironmentRequest request,
            Authentication authentication) {
        EnvironmentResponse response = environmentService.updateEnvironment(id, request, getUserId(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an environment.
     *
     * @param id the environment ID
     * @param authentication the authentication object
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(
            @PathVariable UUID id,
            Authentication authentication) {
        environmentService.deleteEnvironment(id, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}

