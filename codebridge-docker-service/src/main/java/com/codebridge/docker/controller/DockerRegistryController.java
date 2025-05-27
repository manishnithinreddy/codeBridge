package com.codebridge.docker.controller;

import com.codebridge.docker.model.DockerImage;
import com.codebridge.docker.model.DockerRegistry;
import com.codebridge.docker.service.DockerRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Docker registry operations.
 */
@RestController
@RequestMapping("/api/docker/registries")
@Tag(name = "Docker Registries", description = "APIs for Docker registry operations")
@SecurityRequirement(name = "bearerAuth")
public class DockerRegistryController {

    private final DockerRegistryService registryService;

    @Autowired
    public DockerRegistryController(DockerRegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * Get all Docker registries.
     *
     * @return List of Docker registries
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get all Docker registries",
        description = "Get a list of all Docker registries",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerRegistry.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<List<DockerRegistry>> getAllRegistries() {
        List<DockerRegistry> registries = registryService.getAllRegistries();
        return ResponseEntity.ok(registries);
    }

    /**
     * Get a Docker registry by ID.
     *
     * @param id The registry ID
     * @return The Docker registry
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get a Docker registry",
        description = "Get a Docker registry by its ID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerRegistry.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Registry not found")
        }
    )
    public ResponseEntity<DockerRegistry> getRegistryById(
            @Parameter(description = "Registry ID", required = true)
            @PathVariable String id) {
        
        DockerRegistry registry = registryService.getRegistryById(id);
        
        if (registry == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(registry);
    }

    /**
     * Add a new Docker registry.
     *
     * @param registry The Docker registry to add
     * @return The added Docker registry
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Add a Docker registry",
        description = "Add a new Docker registry",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerRegistry.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "400", description = "Bad request")
        }
    )
    public ResponseEntity<DockerRegistry> addRegistry(
            @Parameter(description = "Docker registry details", required = true)
            @RequestBody DockerRegistry registry) {
        
        DockerRegistry addedRegistry = registryService.addRegistry(registry);
        return ResponseEntity.ok(addedRegistry);
    }

    /**
     * Update a Docker registry.
     *
     * @param id The registry ID
     * @param registry The updated Docker registry
     * @return The updated Docker registry
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Update a Docker registry",
        description = "Update an existing Docker registry",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerRegistry.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Registry not found")
        }
    )
    public ResponseEntity<DockerRegistry> updateRegistry(
            @Parameter(description = "Registry ID", required = true)
            @PathVariable String id,
            
            @Parameter(description = "Updated Docker registry details", required = true)
            @RequestBody DockerRegistry registry) {
        
        DockerRegistry updatedRegistry = registryService.updateRegistry(id, registry);
        
        if (updatedRegistry == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedRegistry);
    }

    /**
     * Delete a Docker registry.
     *
     * @param id The registry ID
     * @return Success status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Delete a Docker registry",
        description = "Delete a Docker registry by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Registry not found")
        }
    )
    public ResponseEntity<Void> deleteRegistry(
            @Parameter(description = "Registry ID", required = true)
            @PathVariable String id) {
        
        boolean deleted = registryService.deleteRegistry(id);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Test connection to a Docker registry.
     *
     * @param registry The Docker registry to test
     * @return Success status
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Test registry connection",
        description = "Test connection to a Docker registry",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "400", description = "Bad request")
        }
    )
    public ResponseEntity<Map<String, Boolean>> testRegistryConnection(
            @Parameter(description = "Docker registry details", required = true)
            @RequestBody DockerRegistry registry) {
        
        boolean success = registryService.testRegistryConnection(registry);
        
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * Get images from a Docker registry.
     *
     * @param registryId The registry ID
     * @param namespace The namespace (optional)
     * @return List of Docker images
     */
    @GetMapping("/{registryId}/images")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get registry images",
        description = "Get images from a Docker registry",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerImage.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Registry not found")
        }
    )
    public ResponseEntity<List<DockerImage>> getImages(
            @Parameter(description = "Registry ID", required = true)
            @PathVariable String registryId,
            
            @Parameter(description = "Namespace (optional)")
            @RequestParam(required = false) String namespace) {
        
        List<DockerImage> images = registryService.getImages(registryId, namespace);
        
        return ResponseEntity.ok(images);
    }

    /**
     * Get tags for a Docker image.
     *
     * @param registryId The registry ID
     * @param imageName The image name
     * @return List of tags
     */
    @GetMapping("/{registryId}/images/{imageName}/tags")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get image tags",
        description = "Get tags for a Docker image",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Registry or image not found")
        }
    )
    public ResponseEntity<List<String>> getImageTags(
            @Parameter(description = "Registry ID", required = true)
            @PathVariable String registryId,
            
            @Parameter(description = "Image name", required = true)
            @PathVariable String imageName) {
        
        List<String> tags = registryService.getImageTags(registryId, imageName);
        
        return ResponseEntity.ok(tags);
    }
}

