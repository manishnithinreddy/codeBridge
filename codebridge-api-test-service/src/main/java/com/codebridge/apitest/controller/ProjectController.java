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

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // Helper method to extract platform user ID from authentication
    private Long getPlatformUserId(Authentication authentication) {
        if (authentication == null) {
            // For testing purposes, return a fixed ID
            return 1L;
        }
        return Long.parseLong(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest projectRequest,
                                                        Authentication authentication) {
        Long platformUserId = getPlatformUserId(authentication);
        ProjectResponse createdProject = projectService.createProject(projectRequest, platformUserId);
        return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId,
                                                         Authentication authentication) {
        Long platformUserId = getPlatformUserId(authentication);
        ProjectResponse project = projectService.getProjectById(projectId, platformUserId);
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(Authentication authentication) {
        Long platformUserId = getPlatformUserId(authentication);
        List<ProjectResponse> projects = projectService.getAllProjects(platformUserId);
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId,
                                                        @Valid @RequestBody ProjectRequest projectRequest,
                                                        Authentication authentication) {
        Long platformUserId = getPlatformUserId(authentication);
        ProjectResponse updatedProject = projectService.updateProject(projectId, projectRequest, platformUserId);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                             Authentication authentication) {
        Long platformUserId = getPlatformUserId(authentication);
        projectService.deleteProject(projectId, platformUserId);
        return ResponseEntity.noContent().build();
    }
}

