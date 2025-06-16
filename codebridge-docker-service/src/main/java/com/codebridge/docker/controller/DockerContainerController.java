package com.codebridge.docker.controller;

import com.codebridge.docker.model.ContainerInfo;
import com.codebridge.docker.service.DockerContainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Docker container operations.
 */
@Slf4j
@RestController
@RequestMapping("/containers")
@Tag(name = "Docker Containers", description = "Docker container operations")
public class DockerContainerController {

    private final DockerContainerService dockerContainerService;

    public DockerContainerController(DockerContainerService dockerContainerService) {
        this.dockerContainerService = dockerContainerService;
    }

    /**
     * Gets all containers.
     *
     * @param showAll Whether to show all containers (including stopped ones)
     * @return ResponseEntity containing list of container information
     */
    @GetMapping
    @Operation(
        summary = "Get all containers",
        description = "Retrieves all Docker containers, optionally including stopped containers",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Containers retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContainerInfo.class))
            )
        }
    )
    public ResponseEntity<List<ContainerInfo>> getContainers(
            @Parameter(description = "Whether to show all containers (including stopped ones)")
            @RequestParam(defaultValue = "false") boolean showAll) {
        log.info("Getting all containers, showAll: {}", showAll);
        List<ContainerInfo> containers = dockerContainerService.getContainers(showAll);
        return ResponseEntity.ok(containers);
    }

    /**
     * Gets a specific container by ID or name.
     *
     * @param containerIdOrName ID or name of the container
     * @return ResponseEntity containing container information
     */
    @GetMapping("/{containerIdOrName}")
    @Operation(
        summary = "Get a specific container",
        description = "Retrieves a specific Docker container by ID or name",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Container retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContainerInfo.class))
            ),
            @ApiResponse(responseCode = "404", description = "Container not found")
        }
    )
    public ResponseEntity<ContainerInfo> getContainer(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName) {
        log.info("Getting container: {}", containerIdOrName);
        ContainerInfo container = dockerContainerService.getContainer(containerIdOrName);
        
        if (container == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(container);
    }

    /**
     * Creates a new container.
     *
     * @param image Image to use for the container
     * @param name Name for the container
     * @param env Environment variables
     * @param ports Port mappings
     * @param volumes Volume mappings
     * @param cmd Command to run
     * @return ResponseEntity containing created container information
     */
    @PostMapping
    @Operation(
        summary = "Create a new container",
        description = "Creates a new Docker container with the specified parameters",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Container created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContainerInfo.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
        }
    )
    public ResponseEntity<ContainerInfo> createContainer(
            @Parameter(description = "Image to use for the container", required = true)
            @RequestParam String image,
            @Parameter(description = "Name for the container")
            @RequestParam(required = false) String name,
            @Parameter(description = "Environment variables")
            @RequestParam(required = false) Map<String, String> env,
            @Parameter(description = "Port mappings")
            @RequestParam(required = false) Map<String, String> ports,
            @Parameter(description = "Volume mappings")
            @RequestParam(required = false) Map<String, String> volumes,
            @Parameter(description = "Command to run")
            @RequestParam(required = false) String[] cmd) {
        log.info("Creating container with image: {}, name: {}", image, name);
        ContainerInfo container = dockerContainerService.createContainer(image, name, env, ports, volumes, cmd);
        
        if (container == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(container);
    }

    /**
     * Starts a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/{containerIdOrName}/start")
    @Operation(
        summary = "Start a container",
        description = "Starts a Docker container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Container started successfully"),
            @ApiResponse(responseCode = "404", description = "Container not found"),
            @ApiResponse(responseCode = "500", description = "Failed to start container")
        }
    )
    public ResponseEntity<Void> startContainer(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName) {
        log.info("Starting container: {}", containerIdOrName);
        boolean success = dockerContainerService.startContainer(containerIdOrName);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Stops a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param timeout Timeout in seconds before killing the container
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/{containerIdOrName}/stop")
    @Operation(
        summary = "Stop a container",
        description = "Stops a Docker container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Container stopped successfully"),
            @ApiResponse(responseCode = "404", description = "Container not found"),
            @ApiResponse(responseCode = "500", description = "Failed to stop container")
        }
    )
    public ResponseEntity<Void> stopContainer(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName,
            @Parameter(description = "Timeout in seconds before killing the container")
            @RequestParam(defaultValue = "10") int timeout) {
        log.info("Stopping container: {} with timeout: {}", containerIdOrName, timeout);
        boolean success = dockerContainerService.stopContainer(containerIdOrName, timeout);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Restarts a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param timeout Timeout in seconds before killing the container
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/{containerIdOrName}/restart")
    @Operation(
        summary = "Restart a container",
        description = "Restarts a Docker container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Container restarted successfully"),
            @ApiResponse(responseCode = "404", description = "Container not found"),
            @ApiResponse(responseCode = "500", description = "Failed to restart container")
        }
    )
    public ResponseEntity<Void> restartContainer(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName,
            @Parameter(description = "Timeout in seconds before killing the container")
            @RequestParam(defaultValue = "10") int timeout) {
        log.info("Restarting container: {} with timeout: {}", containerIdOrName, timeout);
        boolean success = dockerContainerService.restartContainer(containerIdOrName, timeout);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Gets logs for a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param tail Number of lines to show from the end of the logs
     * @param timestamps Whether to show timestamps
     * @return ResponseEntity containing container logs
     */
    @GetMapping("/{containerIdOrName}/logs")
    @Operation(
        summary = "Get logs for a container",
        description = "Retrieves the logs for a Docker container",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Container logs retrieved successfully",
                content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(responseCode = "404", description = "Container not found")
        }
    )
    public ResponseEntity<String> getContainerLogs(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName,
            @Parameter(description = "Number of lines to show from the end of the logs")
            @RequestParam(defaultValue = "100") int tail,
            @Parameter(description = "Whether to show timestamps")
            @RequestParam(defaultValue = "false") boolean timestamps) {
        log.info("Getting logs for container: {}, tail: {}, timestamps: {}", containerIdOrName, tail, timestamps);
        String logs = dockerContainerService.getContainerLogs(containerIdOrName, tail, timestamps);
        return ResponseEntity.ok(logs);
    }

    /**
     * Gets stats for a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return ResponseEntity containing container stats
     */
    @GetMapping("/{containerIdOrName}/stats")
    @Operation(
        summary = "Get stats for a container",
        description = "Retrieves the stats for a Docker container",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Container stats retrieved successfully",
                content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "404", description = "Container not found")
        }
    )
    public ResponseEntity<Map<String, Object>> getContainerStats(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName) {
        log.info("Getting stats for container: {}", containerIdOrName);
        Map<String, Object> stats = dockerContainerService.getContainerStats(containerIdOrName);
        
        if (stats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Removes a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param removeVolumes Whether to remove volumes
     * @param force Whether to force removal
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/{containerIdOrName}")
    @Operation(
        summary = "Remove a container",
        description = "Removes a Docker container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Container removed successfully"),
            @ApiResponse(responseCode = "404", description = "Container not found"),
            @ApiResponse(responseCode = "500", description = "Failed to remove container")
        }
    )
    public ResponseEntity<Void> removeContainer(
            @Parameter(description = "ID or name of the container", required = true)
            @PathVariable String containerIdOrName,
            @Parameter(description = "Whether to remove volumes")
            @RequestParam(defaultValue = "false") boolean removeVolumes,
            @Parameter(description = "Whether to force removal")
            @RequestParam(defaultValue = "false") boolean force) {
        log.info("Removing container: {}, removeVolumes: {}, force: {}", containerIdOrName, removeVolumes, force);
        boolean success = dockerContainerService.removeContainer(containerIdOrName, removeVolumes, force);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }
}

