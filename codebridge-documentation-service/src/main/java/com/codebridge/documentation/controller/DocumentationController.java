package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ApiDocumentation;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing API documentation.
 */
@RestController
@RequestMapping("/api/docs/documentation")
@RequiredArgsConstructor
@Tag(name = "Documentation", description = "API for managing API documentation")
public class DocumentationController {

    private final DocumentationService documentationService;

    /**
     * Get all documentation.
     *
     * @return the list of API documentation
     */
    @GetMapping
    @Operation(summary = "Get all documentation", description = "Returns all API documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved documentation",
                content = @Content(schema = @Schema(implementation = ApiDocumentation.class)))
    })
    public ResponseEntity<List<ApiDocumentation>> getAllDocumentation() {
        return ResponseEntity.ok(documentationService.getAllDocumentation());
    }

    /**
     * Get documentation by ID.
     *
     * @param id the documentation ID
     * @return the API documentation
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get documentation by ID", description = "Returns API documentation by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved documentation",
                content = @Content(schema = @Schema(implementation = ApiDocumentation.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<ApiDocumentation> getDocumentationById(
            @Parameter(description = "Documentation ID") @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(documentationService.getDocumentationById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get documentation by service and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the API documentation
     */
    @GetMapping("/service/{serviceName}/version/{versionName}")
    @Operation(summary = "Get documentation by service and version", 
            description = "Returns API documentation by service name and version name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved documentation",
                content = @Content(schema = @Schema(implementation = ApiDocumentation.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<ApiDocumentation> getDocumentationByServiceAndVersion(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            return ResponseEntity.ok(documentationService.getDocumentationByServiceAndVersion(serviceName, versionName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all documentation for a service.
     *
     * @param serviceName the service name
     * @return the list of API documentation
     */
    @GetMapping("/service/{serviceName}")
    @Operation(summary = "Get all documentation for a service", 
            description = "Returns all API documentation for a service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved documentation",
                content = @Content(schema = @Schema(implementation = ApiDocumentation.class))),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    public ResponseEntity<List<ApiDocumentation>> getAllDocumentationForService(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        try {
            return ResponseEntity.ok(documentationService.getAllDocumentationForService(serviceName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate documentation for a service.
     *
     * @param serviceName the service name
     * @return the generated API documentation
     */
    @PostMapping("/service/{serviceName}/generate")
    @Operation(summary = "Generate documentation for a service", 
            description = "Generates API documentation for a service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated documentation",
                content = @Content(schema = @Schema(implementation = ApiDocumentation.class))),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Error generating documentation")
    })
    public ResponseEntity<ApiDocumentation> generateDocumentationForService(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        try {
            ServiceDefinition service = documentationService.getServiceByName(serviceName);
            ApiDocumentation documentation = documentationService.generateDocumentationForService(service);
            
            if (documentation == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
            
            return ResponseEntity.ok(documentation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Delete documentation.
     *
     * @param id the documentation ID
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete documentation", description = "Deletes API documentation by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted documentation"),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<Void> deleteDocumentation(
            @Parameter(description = "Documentation ID") @PathVariable UUID id) {
        try {
            documentationService.deleteDocumentation(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

