package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ApiVersion;
import com.codebridge.documentation.model.ServiceDefinition;
import com.codebridge.documentation.model.VersionStatus;
import com.codebridge.documentation.service.DocumentationService;
import com.codebridge.documentation.service.VersioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing API versions.
 */
@RestController
@RequestMapping("/api/docs/versions")
@RequiredArgsConstructor
@Tag(name = "Versions", description = "API for managing API versions")
public class VersionController {

    private final VersioningService versioningService;
    private final DocumentationService documentationService;

    /**
     * Get all versions for a service.
     *
     * @param serviceName the service name
     * @return the list of API versions
     */
    @GetMapping("/service/{serviceName}")
    @Operation(summary = "Get all versions for a service", description = "Returns all API versions for a service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved versions",
                content = @Content(schema = @Schema(implementation = ApiVersion.class))),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<List<ApiVersion>> getAllVersionsForService(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        try {
            ServiceDefinition service = documentationService.getServiceByName(serviceName);
            return ResponseEntity.ok(versioningService.getAllVersions(service));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get a version by ID.
     *
     * @param id the version ID
     * @return the API version
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a version by ID", description = "Returns an API version by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved version",
                content = @Content(schema = @Schema(implementation = ApiVersion.class))),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public ResponseEntity<ApiVersion> getVersionById(
            @Parameter(description = "Version ID") @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(versioningService.getVersionById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get a version by service and name.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the API version
     */
    @GetMapping("/service/{serviceName}/version/{versionName}")
    @Operation(summary = "Get a version by service and name", 
            description = "Returns an API version by service name and version name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved version",
                content = @Content(schema = @Schema(implementation = ApiVersion.class))),
        @ApiResponse(responseCode = "404", description = "Service or version not found")
    })
    public ResponseEntity<ApiVersion> getVersionByName(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            ServiceDefinition service = documentationService.getServiceByName(serviceName);
            return ResponseEntity.ok(versioningService.getVersionByName(service, versionName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get the latest version for a service.
     *
     * @param serviceName the service name
     * @return the API version
     */
    @GetMapping("/service/{serviceName}/latest")
    @Operation(summary = "Get the latest version for a service", 
            description = "Returns the latest API version for a service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved version",
                content = @Content(schema = @Schema(implementation = ApiVersion.class))),
        @ApiResponse(responseCode = "404", description = "Service not found or no versions available")
    })
    public ResponseEntity<ApiVersion> getLatestVersionForService(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        try {
            ServiceDefinition service = documentationService.getServiceByName(serviceName);
            ApiVersion version = versioningService.getLatestVersion(service);
            
            if (version == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(version);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update version status.
     *
     * @param id the version ID
     * @param status the new status
     * @return the updated API version
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update version status", description = "Updates the status of an API version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated version",
                content = @Content(schema = @Schema(implementation = ApiVersion.class))),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public ResponseEntity<ApiVersion> updateVersionStatus(
            @Parameter(description = "Version ID") @PathVariable UUID id,
            @Parameter(description = "Version status") @RequestParam VersionStatus status) {
        try {
            return ResponseEntity.ok(versioningService.updateVersionStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a version.
     *
     * @param id the version ID
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a version", description = "Deletes an API version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted version"),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public ResponseEntity<Void> deleteVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id) {
        try {
            versioningService.deleteVersion(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

