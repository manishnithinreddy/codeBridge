package com.codebridge.docker.controller;

import com.codebridge.docker.dto.ContainerCreationRequest;
import com.codebridge.docker.dto.ContainerResponse;
import com.codebridge.docker.service.DockerContainerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Controller for Docker container operations.
 */
@RestController
@RequestMapping("/api/containers")
public class DockerContainerController {

    private final DockerContainerService containerService;

    public DockerContainerController(DockerContainerService containerService) {
        this.containerService = containerService;
    }

    /**
     * Creates a new Docker container.
     *
     * @param request the container creation request
     * @return the created container
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ContainerResponse> createContainer(@Valid @RequestBody ContainerCreationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        UUID teamId = null;
        
        if (authentication.getDetails() != null && authentication.getDetails() instanceof String) {
            teamId = UUID.fromString((String) authentication.getDetails());
        }
        
        ContainerResponse container = containerService.createContainer(request, userId, teamId);
        return new ResponseEntity<>(container, HttpStatus.CREATED);
    }

    /**
     * Gets all containers for the authenticated user.
     *
     * @return the list of containers
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ContainerResponse>> getContainers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        
        List<ContainerResponse> containers = containerService.getContainers(userId);
        return ResponseEntity.ok(containers);
    }

    /**
     * Gets a container by ID.
     *
     * @param id the container ID
     * @return the container
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ContainerResponse> getContainer(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        
        ContainerResponse container = containerService.getContainer(id, userId);
        return ResponseEntity.ok(container);
    }

    /**
     * Starts a container.
     *
     * @param id the container ID
     * @return the updated container
     */
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ContainerResponse> startContainer(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        
        ContainerResponse container = containerService.startContainer(id, userId);
        return ResponseEntity.ok(container);
    }

    /**
     * Stops a container.
     *
     * @param id the container ID
     * @return the updated container
     */
    @PutMapping("/{id}/stop")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ContainerResponse> stopContainer(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        
        ContainerResponse container = containerService.stopContainer(id, userId);
        return ResponseEntity.ok(container);
    }

    /**
     * Removes a container.
     *
     * @param id the container ID
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeContainer(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        
        containerService.removeContainer(id, userId);
        return ResponseEntity.noContent().build();
    }
}

