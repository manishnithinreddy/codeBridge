package com.codebridge.docker.controller;

import com.codebridge.docker.model.DockerContainer;
import com.codebridge.docker.model.DockerLog;
import com.codebridge.docker.service.DockerContainerService;
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
 * Controller for Docker container operations.
 */
@RestController
@RequestMapping("/api/docker/contexts/{contextId}/containers")
@Tag(name = "Docker Containers", description = "APIs for Docker container operations")
@SecurityRequirement(name = "bearerAuth")
public class DockerContainerController {

    private final DockerContainerService containerService;

    @Autowired
    public DockerContainerController(DockerContainerService containerService) {
        this.containerService = containerService;
    }

    /**
     * Get all containers in a Docker context.
     *
     * @param contextId The context ID
     * @param showAll Whether to show all containers (including stopped ones)
     * @return List of Docker containers
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get all containers",
        description = "Get a list of all containers in a Docker context",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContainer.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context not found")
        }
    )
    public ResponseEntity<List<DockerContainer>> getContainers(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Show all containers (including stopped ones)")
            @RequestParam(required = false, defaultValue = "false") boolean showAll) {
        
        List<DockerContainer> containers = containerService.getContainers(contextId, showAll);
        
        return ResponseEntity.ok(containers);
    }

    /**
     * Get a container by ID.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return The Docker container
     */
    @GetMapping("/{containerId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get a container",
        description = "Get a container by its ID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerContainer.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<DockerContainer> getContainer(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId) {
        
        DockerContainer container = containerService.getContainer(contextId, containerId);
        
        if (container == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(container);
    }

    /**
     * Start a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return Success status
     */
    @PostMapping("/{containerId}/start")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Start a container",
        description = "Start a container by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> startContainer(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId) {
        
        boolean started = containerService.startContainer(contextId, containerId);
        
        return ResponseEntity.ok(Map.of("success", started));
    }

    /**
     * Stop a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return Success status
     */
    @PostMapping("/{containerId}/stop")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Stop a container",
        description = "Stop a container by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> stopContainer(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId) {
        
        boolean stopped = containerService.stopContainer(contextId, containerId);
        
        return ResponseEntity.ok(Map.of("success", stopped));
    }

    /**
     * Restart a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return Success status
     */
    @PostMapping("/{containerId}/restart")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Restart a container",
        description = "Restart a container by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> restartContainer(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId) {
        
        boolean restarted = containerService.restartContainer(contextId, containerId);
        
        return ResponseEntity.ok(Map.of("success", restarted));
    }

    /**
     * Remove a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @param force Whether to force removal
     * @return Success status
     */
    @DeleteMapping("/{containerId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Remove a container",
        description = "Remove a container by its ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> removeContainer(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId,
            
            @Parameter(description = "Force removal")
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        
        boolean removed = containerService.removeContainer(contextId, containerId, force);
        
        return ResponseEntity.ok(Map.of("success", removed));
    }

    /**
     * Get container logs.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @param tail Number of lines to show from the end of the logs
     * @return List of Docker logs
     */
    @GetMapping("/{containerId}/logs")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get container logs",
        description = "Get logs for a container",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = @Content(schema = @Schema(implementation = DockerLog.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<List<DockerLog>> getContainerLogs(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId,
            
            @Parameter(description = "Number of lines to show from the end of the logs")
            @RequestParam(required = false, defaultValue = "100") int tail) {
        
        List<DockerLog> logs = containerService.getContainerLogs(contextId, containerId, tail);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Stream container logs.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return Success status
     */
    @PostMapping("/{containerId}/logs/stream")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Stream container logs",
        description = "Start streaming logs for a container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> streamContainerLogs(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId) {
        
        boolean started = containerService.streamContainerLogs(contextId, containerId);
        
        return ResponseEntity.ok(Map.of("success", started));
    }

    /**
     * Stop streaming container logs.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return Success status
     */
    @PostMapping("/{containerId}/logs/stop-stream")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Stop streaming container logs",
        description = "Stop streaming logs for a container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Context or container not found")
        }
    )
    public ResponseEntity<Map<String, Boolean>> stopLogStream(
            @Parameter(description = "Context ID", required = true)
            @PathVariable String contextId,
            
            @Parameter(description = "Container ID", required = true)
            @PathVariable String containerId) {
        
        boolean stopped = containerService.stopLogStream(contextId, containerId);
        
        return ResponseEntity.ok(Map.of("success", stopped));
    }
}

