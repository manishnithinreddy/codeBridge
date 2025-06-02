package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // This case should ideally be handled by Spring Security global exception handler
            // if authentication is required for the endpoint.
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest projectRequest,
                                                         Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        ProjectResponse createdProject = projectService.createProject(projectRequest, platformUserId);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID projectId,
                                                          Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        ProjectResponse project = projectService.getProjectByIdForUser(projectId, platformUserId);
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        List<ProjectResponse> projects = projectService.listProjectsForUser(platformUserId);
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID projectId,
                                                         @Valid @RequestBody ProjectRequest projectRequest,
                                                         Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        ProjectResponse updatedProject = projectService.updateProject(projectId, projectRequest, platformUserId);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId,
                                              Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        projectService.deleteProject(projectId, platformUserId);
        return ResponseEntity.noContent().build();
    }
}
