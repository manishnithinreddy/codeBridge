package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Project; // Updated to use apitest.model
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.model.enums.SharePermissionLevel; // Updated to use apitest.model.enums
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.DuplicateResourceException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public ProjectResponse createProject(ProjectRequest projectRequest, Long userId) {
        if (projectRepository.existsByNameAndUserId(projectRequest.getName(), userId)) {
            throw new DuplicateResourceException("Project with name '" + projectRequest.getName() + "' already exists for this user.");
        }
        Project project = new Project();
        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        project.setUserId(userId);
        Project savedProject = projectRepository.save(project);
        return mapToProjectResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Long userId) {
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, userId);
        if (effectivePermission == null) {
            throw new ResourceNotFoundException("Project", "id", projectId + " or access denied.");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(Long userId) {
        Set<ProjectResponse> projectsSet = new HashSet<>();

        projectRepository.findByUserId(userId).stream()
                .map(this::mapToProjectResponse)
                .forEach(projectsSet::add);

        projectSharingService.listSharedProjectsForUser(userId)
                .forEach(projectsSet::add);

        List<ProjectResponse> combinedProjects = new ArrayList<>(projectsSet);
        combinedProjects.sort(Comparator.comparing(ProjectResponse::getName, String.CASE_INSENSITIVE_ORDER));
        return combinedProjects;
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest projectRequest, Long userId) {
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, userId);
        if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to update project " + projectId);
        }

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getName().equals(projectRequest.getName()) &&
            project.getUserId().equals(userId) &&
            projectRepository.existsByNameAndUserId(projectRequest.getName(), userId)) {
            throw new DuplicateResourceException("Another project with name '" + projectRequest.getName() + "' already exists for this user.");
        }

        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        Project updatedProject = projectRepository.save(project);
        return mapToProjectResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getUserId().equals(userId)) {
            throw new AccessDeniedException("User " + userId + " is not the owner of project " + projectId + " and cannot delete it.");
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
                project.getUserId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}

