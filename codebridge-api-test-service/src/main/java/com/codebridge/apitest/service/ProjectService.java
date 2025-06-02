package com.codebridge.apitest.service;

import com.codebridge.apitester.model.Project; // Model from apitester
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.exception.DuplicateResourceException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository; // Repository from apitest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest projectRequest, UUID platformUserId) {
        if (projectRepository.existsByNameAndPlatformUserId(projectRequest.getName(), platformUserId)) {
            throw new DuplicateResourceException("Project with name '" + projectRequest.getName() + "' already exists for this user.");
        }
        Project project = new Project();
        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        project.setPlatformUserId(platformUserId);
        // createdAt and updatedAt are set by @CreationTimestamp and @UpdateTimestamp

        Project savedProject = projectRepository.save(project);
        return mapToProjectResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectByIdForUser(UUID projectId, UUID platformUserId) {
        Project project = projectRepository.findByIdAndPlatformUserId(projectId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for this user."));
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjectsForUser(UUID platformUserId) {
        return projectRepository.findByPlatformUserId(platformUserId).stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest projectRequest, UUID platformUserId) {
        Project project = projectRepository.findByIdAndPlatformUserId(projectId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for this user."));

        // Check for duplicate name if the name is being changed
        if (!project.getName().equals(projectRequest.getName()) &&
            projectRepository.existsByNameAndPlatformUserId(projectRequest.getName(), platformUserId)) {
            throw new DuplicateResourceException("Another project with name '" + projectRequest.getName() + "' already exists for this user.");
        }

        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        // platformUserId, createdAt remain unchanged, updatedAt will be handled by @UpdateTimestamp

        Project updatedProject = projectRepository.save(project);
        return mapToProjectResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID platformUserId) {
        Project project = projectRepository.findByIdAndPlatformUserId(projectId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for this user, cannot delete."));
        // Deletion of associated collections is handled by CascadeType.ALL on Project entity.
        // Deletion of associated ShareGrants will be handled by ProjectSharingService or direct repository call in a later phase.
        projectRepository.delete(project);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        if (project == null) {
            return null;
        }
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getPlatformUserId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
