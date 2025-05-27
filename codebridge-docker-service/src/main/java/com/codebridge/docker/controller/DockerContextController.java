package com.codebridge.docker.controller;

import com.codebridge.docker.model.DockerContext;
import com.codebridge.docker.service.DockerContextService;
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
 * Controller for Docker context operations.
 */
@RestController
@RequestMapping("/api/docker/contexts")
@Tag(name = "Docker Contexts", description = "APIs for Docker context operations")
@SecurityRequirement(name = "bearerAuth")
public class DockerContextController {

    private final DockerContextService contextService;

    @Autowired
    public DockerContextController(DockerContextService contextService) {
        this.contextService = contextService;
    }

    /**
     * Get all Docker contexts.
     *
     * @return List of Docker contexts
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get all Docker contexts",
        description = "Get a list of all Docker contexts",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContext.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<List<DockerContext>> getAllContexts() {
        List<DockerContext> contexts = contextService.getAllContexts();
        return ResponseEntity.ok(contexts);
    }

    /**
     * Get a Docker context by ID.
     *
     * @param id The context ID
     * @return The Docker context
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get a Docker context",
        description = "Get a Docker context by its ID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContext.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context not found")
        }
    )
    public ResponseEntity<DockerContext> getContextById(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String id) {
        
        DockerContext context = contextService.getContextById(id);
        
        if (context == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(context);
    }

    /**
     * Add a new Docker context.
     *
     * @param context The Docker context to add
     * @return The added Docker context
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Add a Docker context",
        description = "Add a new Docker context",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContext.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "400", description = "Bad request")
        }
    )
    public ResponseEntity<DockerContext> addContext(
            @Parameter(description = "Docker context details", required = true)
            @RequestBody DockerContext context) {
        
        DockerContext addedContext = contextService.addContext(context);
        return ResponseEntity.ok(addedContext);
    }

    /**
     * Update a Docker context.
     *
     * @param id The context ID
     * @param context The updated Docker context
     * @return The updated Docker context
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Update a Docker context",
        description = "Update an existing Docker context",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContext.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context not found")
        }
    )
    public ResponseEntity<DockerContext> updateContext(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String id,
            
            @Parameter(description = "Updated Docker context details", required = true)
            @RequestBody DockerContext context) {
        
        DockerContext updatedContext = contextService.updateContext(id, context);
        
        if (updatedContext == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedContext);
    }

    /**
     * Delete a Docker context.
     *
     * @param id The context ID
     * @return Success status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Delete a Docker context",
        description = "Delete a Docker context by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context not found")
        }
    )
    public ResponseEntity<Void> deleteContext(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String id) {
        
        boolean deleted = contextService.deleteContext(id);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Set a Docker context as default.
     *
     * @param id The context ID
     * @return The updated Docker context
     */
    @PostMapping("/{id}/set-default")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Set default context",
        description = "Set a Docker context as the default context",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContext.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context not found")
        }
    )
    public ResponseEntity<DockerContext> setDefaultContext(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String id) {
        
        DockerContext context = contextService.setDefaultContext(id);
        
        if (context == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(context);
    }

    /**
     * Test connection to a Docker context.
     *
     * @param context The Docker context to test
     * @return Success status
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Test context connection",
        description = "Test connection to a Docker context",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "400", description = "Bad request")
        }
    )
    public ResponseEntity<Map<String, Boolean>> testContextConnection(
            @Parameter(description = "Docker context details", required = true)
            @RequestBody DockerContext context) {
        
        boolean success = contextService.testContextConnection(context);
        
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * Execute a Docker command in a specific context.
     *
     * @param contextId The context ID
     * @param command The Docker command to execute
     * @return The command output
     */
    @PostMapping("/{contextId}/execute")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Execute Docker command",
        description = "Execute a Docker command in a specific context",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context not found")
        }
    )
    public ResponseEntity<String> executeCommand(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Docker command", required = true)
            @RequestBody List<String> command) {
        
        String output = contextService.executeCommand(contextId, command);
        
        return ResponseEntity.ok(output);
    }

    /**
     * Stop a running Docker command.
     *
     * @param contextId The context ID
     * @return Success status
     */
    @PostMapping("/{contextId}/stop-command")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Stop Docker command",
        description = "Stop a running Docker command",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or command not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> stopCommand(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId) {
        
        boolean stopped = contextService.stopCommand(contextId);
        
        return ResponseEntity.ok(Map.of("success", stopped));
    }
}

