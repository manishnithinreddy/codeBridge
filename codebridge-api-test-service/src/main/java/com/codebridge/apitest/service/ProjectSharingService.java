package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Project; // Updated to use apitest.model
import com.codebridge.apitest.model.ShareGrant; // Updated to use apitest.model
import com.codebridge.apitest.model.enums.SharePermissionLevel; // Updated to use apitest.model.enums
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.dto.ShareGrantRequest;
import com.codebridge.apitest.dto.ShareGrantResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository;
import com.codebridge.apitest.repository.ShareGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectSharingService {

    private final ShareGrantRepository shareGrantRepository;
    private final ProjectRepository projectRepository;
    // private final ProjectService projectService; // For mapping Project to ProjectResponse, if preferred

    public ProjectSharingService(ShareGrantRepository shareGrantRepository,
                                 ProjectRepository projectRepository
                                 /* ProjectService projectService */) {
        this.shareGrantRepository = shareGrantRepository;
        this.projectRepository = projectRepository;
        // this.projectService = projectService;
    }

    @Transactional
    public ShareGrantResponse grantProjectAccess(UUID projectId, ShareGrantRequest requestDto, UUID granterUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.getPlatformUserId().equals(granterUserId)) {
            throw new AccessDeniedException("User " + granterUserId + " does not own project " + projectId);
        }

        if (granterUserId.equals(requestDto.getGranteeUserId())) {
            throw new IllegalArgumentException("Cannot share project with oneself.");
        }

        ShareGrant shareGrant = shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, requestDto.getGranteeUserId())
                .orElse(new ShareGrant());

        shareGrant.setProject(project);
        shareGrant.setGranteeUserId(requestDto.getGranteeUserId());
        shareGrant.setPermissionLevel(requestDto.getPermissionLevel());
        shareGrant.setGrantedByUserId(granterUserId); // Update granter if it's an existing grant being modified

        ShareGrant savedShareGrant = shareGrantRepository.save(shareGrant);
        return mapToShareGrantResponse(savedShareGrant);
    }

    @Transactional
    public void revokeProjectAccess(UUID projectId, UUID granteeUserId, UUID revokerUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.getPlatformUserId().equals(revokerUserId)) {
            throw new AccessDeniedException("User " + revokerUserId + " does not own project " + projectId);
        }
        // Ensure a grant exists before trying to delete, or handle it gracefully if repo method doesn't throw.
        // The deleteByProjectIdAndGranteeUserId method in the repository is void, so it won't indicate if a row was deleted.
        // For atomicity and to prevent errors if the grant is already gone, this is usually fine.
        shareGrantRepository.deleteByProjectIdAndGranteeUserId(projectId, granteeUserId);
    }

    @Transactional(readOnly = true)
    public SharePermissionLevel getEffectivePermission(UUID projectId, UUID platformUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (project.getPlatformUserId().equals(platformUserId)) {
            return SharePermissionLevel.CAN_EDIT; // Owner has full edit rights (implicitly)
        }

        return shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, platformUserId)
                .map(ShareGrant::getPermissionLevel)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listSharedProjectsForUser(UUID platformUserId) {
        List<ShareGrant> grants = shareGrantRepository.findByGranteeUserId(platformUserId);
        return grants.stream()
                .map(ShareGrant::getProject)
                .distinct() // Ensure unique projects if multiple grants somehow point to same project (should not happen with current logic)
                .map(this::mapProjectToProjectResponse) // Use a local mapper or inject ProjectService for its mapper
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShareGrantResponse> listUsersForProject(UUID projectId, UUID platformUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.getPlatformUserId().equals(platformUserId)) {
            throw new AccessDeniedException("User " + platformUserId + " does not own project " + projectId + ". Cannot list shares.");
        }

        List<ShareGrant> grants = shareGrantRepository.findByProjectId(projectId);
        return grants.stream()
                .map(this::mapToShareGrantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllGrantsForProject(UUID projectId) {
        // This method is typically called by ProjectService when a project is deleted.
        // It assumes ownership/permission to delete the project (and thus its grants)
        // has already been verified by the calling service.
        shareGrantRepository.deleteByProjectId(projectId);
    }

    // Helper to map Project to ProjectResponse (alternative to injecting ProjectService)
    private ProjectResponse mapProjectToProjectResponse(Project project) {
        if (project == null) return null;
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getPlatformUserId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ShareGrantResponse mapToShareGrantResponse(ShareGrant shareGrant) {
        if (shareGrant == null) return null;
        return new ShareGrantResponse(
                shareGrant.getId(),
                shareGrant.getProject().getId(),
                shareGrant.getProject().getName(), // Include project name
                shareGrant.getGranteeUserId(),
                shareGrant.getPermissionLevel(),
                shareGrant.getGrantedByUserId(),
                shareGrant.getCreatedAt()
        );
    }
}
