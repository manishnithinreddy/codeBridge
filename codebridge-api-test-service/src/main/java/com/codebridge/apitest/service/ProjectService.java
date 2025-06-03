package com.codebridge.apitest.service;

import com.codebridge.apitester.model.Project; // Model from apitester
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitester.model.enums.SharePermissionLevel; // Added
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.exception.AccessDeniedException; // Added
import com.codebridge.apitest.exception.DuplicateResourceException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository; // Repository from apitest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set; // Added
import java.util.HashSet; // Added
import java.util.ArrayList; // Added
import java.util.Comparator; // Added for sorting

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectSharingService projectSharingService; // Added

    public ProjectService(ProjectRepository projectRepository, ProjectSharingService projectSharingService) { // Added
        this.projectRepository = projectRepository;
        this.projectSharingService = projectSharingService; // Added
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest projectRequest, UUID platformUserId) {
        // No change to createProject's authorization, owner creates it.
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
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
        if (effectivePermission == null) {
            throw new ResourceNotFoundException("Project not found with id " + projectId + " or access denied.");
        }
        // If permission exists (any level), fetch and return
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId)); // Should not happen if permission was found
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjectsForUser(UUID platformUserId) {
        Set<ProjectResponse> projectsSet = new HashSet<>();

        // Add directly owned projects
        projectRepository.findByPlatformUserId(platformUserId).stream()
                .map(this::mapToProjectResponse)
                .forEach(projectsSet::add);

        // Add projects shared with the user
        projectSharingService.listSharedProjectsForUser(platformUserId)
                .forEach(projectsSet::add);

        // Convert set to list and sort for consistent ordering, e.g., by name or createdAt
        List<ProjectResponse> combinedProjects = new ArrayList<>(projectsSet);
        combinedProjects.sort(Comparator.comparing(ProjectResponse::getName, String.CASE_INSENSITIVE_ORDER));
        return combinedProjects;
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest projectRequest, UUID platformUserId) {
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
        if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to update project " + projectId);
        }

        Project project = projectRepository.findById(projectId) // Already checked access, now fetch for update
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        // Check for duplicate name if the name is being changed (only if user is owner, or adjust logic)
        // This check needs to be careful not to block an edit by a shared user if the name conflict is with another user's project.
        // For now, assume this check is primarily for the owner.
        if (!project.getName().equals(projectRequest.getName()) &&
            project.getPlatformUserId().equals(platformUserId) && // Only check for owner's potential duplicates
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
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        // IMPORTANT: Only the direct owner can delete a project
        if (!project.getPlatformUserId().equals(platformUserId)) {
            throw new AccessDeniedException("User " + platformUserId + " is not the owner of project " + projectId + " and cannot delete it.");
        }

        // Delete all share grants associated with this project first
        projectSharingService.deleteAllGrantsForProject(projectId);

        // Deletion of associated collections is handled by CascadeType.ALL on Project entity.
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
