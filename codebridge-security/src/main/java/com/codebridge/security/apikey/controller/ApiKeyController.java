package com.codebridge.security.apikey.controller;

import com.codebridge.security.apikey.dto.*;
import com.codebridge.security.apikey.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Controller for API key endpoints.
 */
@RestController
@RequestMapping("/api/apikeys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Creates an API key.
     *
     * @param request The API key creation request
     * @return The API key response
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody ApiKeyCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.createApiKey(request));
    }

    /**
     * Gets API keys for the current user.
     *
     * @return The API keys
     */
    @GetMapping("/me")
    public ResponseEntity<List<ApiKeyDto>> getCurrentUserApiKeys() {
        return ResponseEntity.ok(apiKeyService.getCurrentUserApiKeys());
    }

    /**
     * Gets API keys for a user.
     *
     * @param userId The user ID
     * @return The API keys
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ApiKeyDto>> getUserApiKeys(@PathVariable Long userId) {
        return ResponseEntity.ok(apiKeyService.getUserApiKeys(userId));
    }

    /**
     * Gets an API key by ID.
     *
     * @param id The API key ID
     * @return The API key
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyDto> getApiKeyById(@PathVariable Long id) {
        return ResponseEntity.ok(apiKeyService.getApiKeyById(id));
    }

    /**
     * Revokes an API key.
     *
     * @param id The API key ID
     * @param request The revocation request
     * @return The response entity
     */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyRevocationRequest request) {
        apiKeyService.revokeApiKey(id, request.getReason());
        return ResponseEntity.ok().build();
    }

    /**
     * Rotates an API key.
     *
     * @param id The API key ID
     * @return The API key response
     */
    @PostMapping("/{id}/rotate")
    public ResponseEntity<ApiKeyResponse> rotateApiKey(@PathVariable Long id) {
        return ResponseEntity.ok(apiKeyService.rotateApiKey(id));
    }

    /**
     * Updates API key scopes.
     *
     * @param id The API key ID
     * @param request The scope update request
     * @return The API key DTO
     */
    @PutMapping("/{id}/scopes")
    public ResponseEntity<ApiKeyDto> updateApiKeyScopes(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyScopeUpdateRequest request) {
        return ResponseEntity.ok(apiKeyService.updateApiKeyScopes(id, request.getScopes()));
    }

    /**
     * Updates API key IP restrictions.
     *
     * @param id The API key ID
     * @param request The IP restriction update request
     * @return The API key DTO
     */
    @PutMapping("/{id}/ip-restrictions")
    public ResponseEntity<ApiKeyDto> updateApiKeyIpRestrictions(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyIpRestrictionUpdateRequest request) {
        return ResponseEntity.ok(apiKeyService.updateApiKeyIpRestrictions(id, request.getIpRestrictions()));
    }

    /**
     * Updates API key rate limit.
     *
     * @param id The API key ID
     * @param request The rate limit update request
     * @return The API key DTO
     */
    @PutMapping("/{id}/rate-limit")
    public ResponseEntity<ApiKeyDto> updateApiKeyRateLimit(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyRateLimitUpdateRequest request) {
        return ResponseEntity.ok(apiKeyService.updateApiKeyRateLimit(id, request.getRateLimit()));
    }
}

