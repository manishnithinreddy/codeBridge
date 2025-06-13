package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ServiceDefinition;
import com.codebridge.documentation.service.DocumentationService;
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

/**
 * Controller for managing services.
 */
@RestController
@RequestMapping("/api/docs/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "API for managing services")
public class ServiceController {

    private final DocumentationService documentationService;

    /**
     * Get all services.
     *
     * @return the list of service definitions
     */
    @GetMapping
    @Operation(summary = "Get all services", description = "Returns all registered services")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved services",
                content = @Content(schema = @Schema(implementation = ServiceDefinition.class)))
    })
    public ResponseEntity<List<ServiceDefinition>> getAllServices() {
        return ResponseEntity.ok(documentationService.getAllServices());
    }

    /**
     * Get a service by name.
     *
     * @param name the service name
     * @return the service definition
     */
    @GetMapping("/{name}")
    @Operation(summary = "Get a service by name", description = "Returns a service by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved service",
                content = @Content(schema = @Schema(implementation = ServiceDefinition.class))),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<ServiceDefinition> getServiceByName(
            @Parameter(description = "Service name") @PathVariable String name) {
        try {
            return ResponseEntity.ok(documentationService.getServiceByName(name));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Register a service.
     *
     * @param name the service name
     * @param url the service URL
     * @param contextPath the service context path
     * @param scan whether to scan the service for documentation
     * @return the created service definition
     */
    @PostMapping
    @Operation(summary = "Register a service", description = "Registers a new service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully registered service",
                content = @Content(schema = @Schema(implementation = ServiceDefinition.class)))
    })
    public ResponseEntity<ServiceDefinition> registerService(
            @Parameter(description = "Service name") @RequestParam String name,
            @Parameter(description = "Service URL") @RequestParam String url,
            @Parameter(description = "Service context path") @RequestParam(required = false) String contextPath,
            @Parameter(description = "Whether to scan the service for documentation") @RequestParam(defaultValue = "true") boolean scan) {
        return ResponseEntity.ok(documentationService.registerService(name, url, contextPath, scan));
    }

    /**
     * Enable or disable a service.
     *
     * @param name the service name
     * @param enabled the enabled status
     * @return the updated service definition
     */
    @PutMapping("/{name}/enabled")
    @Operation(summary = "Enable or disable a service", description = "Enables or disables a service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated service",
                content = @Content(schema = @Schema(implementation = ServiceDefinition.class))),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<ServiceDefinition> enableService(
            @Parameter(description = "Service name") @PathVariable String name,
            @Parameter(description = "Enabled status") @RequestParam boolean enabled) {
        try {
            return ResponseEntity.ok(documentationService.enableService(name, enabled));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a service.
     *
     * @param name the service name
     * @return the response entity
     */
    @DeleteMapping("/{name}")
    @Operation(summary = "Delete a service", description = "Deletes a service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted service"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<Void> deleteService(
            @Parameter(description = "Service name") @PathVariable String name) {
        try {
            documentationService.deleteService(name);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

