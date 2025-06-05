package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Project; // Updated to use apitest.model
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.model.enums.SharePermissionLevel; // Updated to use apitest.model.enums
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.DuplicateResourceException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectSharingService projectSharingService;

    public ProjectService(ProjectRepository projectRepository, ProjectSharingService projectSharingService) {
        this.projectRepository = projectRepository;
        this.projectSharingService = projectSharingService;
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
        Project savedProject = projectRepository.save(project);
        return mapToProjectResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectByIdForUser(UUID projectId, UUID platformUserId) {
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
        if (effectivePermission == null) {
            throw new ResourceNotFoundException("Project not found with id " + projectId + " or access denied.");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjectsForUser(UUID platformUserId) {
        Set<ProjectResponse> projectsSet = new HashSet<>();

        projectRepository.findByPlatformUserId(platformUserId).stream()
                .map(this::mapToProjectResponse)
                .forEach(projectsSet::add);

        projectSharingService.listSharedProjectsForUser(platformUserId)
                .forEach(projectsSet::add);

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

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        if (!project.getName().equals(projectRequest.getName()) &&
            project.getPlatformUserId().equals(platformUserId) &&
            projectRepository.existsByNameAndPlatformUserId(projectRequest.getName(), platformUserId)) {
            throw new DuplicateResourceException("Another project with name '" + projectRequest.getName() + "' already exists for this user.");
        }

        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        Project updatedProject = projectRepository.save(project);
        return mapToProjectResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID platformUserId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        if (!project.getPlatformUserId().equals(platformUserId)) {
            throw new AccessDeniedException("User " + platformUserId + " is not the owner of project " + projectId + " and cannot delete it.");
        }

        projectSharingService.deleteAllGrantsForProject(projectId);
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
