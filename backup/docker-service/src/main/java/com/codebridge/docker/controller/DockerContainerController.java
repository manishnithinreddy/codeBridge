package com.codebridge.docker.controller;

import com.codebridge.docker.model.ContainerInfo;
import com.codebridge.docker.service.DockerContainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/containers")
public class DockerContainerController {

    private final DockerContainerService dockerContainerService;

    public DockerContainerController(DockerContainerService dockerContainerService) {
        this.dockerContainerService = dockerContainerService;
    }

    @GetMapping
    public ResponseEntity<List<ContainerInfo>> getContainers(
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        log.info("Getting all containers, all={}", all);
        
        List<ContainerInfo> containers = dockerContainerService.getContainers(all);
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/{containerId}")
    public ResponseEntity<ContainerInfo> getContainer(@PathVariable String containerId) {
        log.info("Getting container: {}", containerId);
        
        ContainerInfo container = dockerContainerService.getContainer(containerId);
        
        if (container == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(container);
    }

    @PostMapping
    public ResponseEntity<String> createContainer(
            @RequestParam String image,
            @RequestParam String name,
            @RequestBody(required = false) Map<String, Object> config) {
        log.info("Creating container with image: {}, name: {}", image, name);
        
        @SuppressWarnings("unchecked")
        Map<String, String> env = config != null ? (Map<String, String>) config.get("env") : null;
        
        @SuppressWarnings("unchecked")
        Map<String, String> ports = config != null ? (Map<String, String>) config.get("ports") : null;
        
        @SuppressWarnings("unchecked")
        Map<String, String> volumes = config != null ? (Map<String, String>) config.get("volumes") : null;
        
        String containerId = dockerContainerService.createContainer(image, name, env, ports, volumes);
        return ResponseEntity.ok(containerId);
    }

    @PostMapping("/{containerId}/start")
    public ResponseEntity<Boolean> startContainer(@PathVariable String containerId) {
        log.info("Starting container: {}", containerId);
        
        boolean success = dockerContainerService.startContainer(containerId);
        
        if (!success) {
            return ResponseEntity.badRequest().body(false);
        }
        
        return ResponseEntity.ok(true);
    }

    @PostMapping("/{containerId}/stop")
    public ResponseEntity<Boolean> stopContainer(@PathVariable String containerId) {
        log.info("Stopping container: {}", containerId);
        
        boolean success = dockerContainerService.stopContainer(containerId);
        
        if (!success) {
            return ResponseEntity.badRequest().body(false);
        }
        
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{containerId}")
    public ResponseEntity<Boolean> removeContainer(
            @PathVariable String containerId,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            @RequestParam(required = false, defaultValue = "false") boolean removeVolumes) {
        log.info("Removing container: {}, force: {}, removeVolumes: {}", containerId, force, removeVolumes);
        
        boolean success = dockerContainerService.removeContainer(containerId, force, removeVolumes);
        
        if (!success) {
            return ResponseEntity.badRequest().body(false);
        }
        
        return ResponseEntity.ok(true);
    }

    @GetMapping("/{containerId}/logs")
    public ResponseEntity<String> getContainerLogs(
            @PathVariable String containerId,
            @RequestParam(required = false) Integer tail,
            @RequestParam(required = false, defaultValue = "false") boolean timestamps,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        log.info("Getting logs for container: {}", containerId);
        
        String logs = dockerContainerService.getContainerLogs(containerId, tail, timestamps, since, until);
        return ResponseEntity.ok(logs);
    }
}

