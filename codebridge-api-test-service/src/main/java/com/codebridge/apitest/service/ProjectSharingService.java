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
import java.util.stream.Collectors;

@Service
public class ProjectSharingService {

    private final ProjectRepository projectRepository;
    private final ShareGrantRepository shareGrantRepository;
    private final ProjectMapper projectMapper;

    public ProjectSharingService(ProjectRepository projectRepository, 
                                ShareGrantRepository shareGrantRepository,
                                ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.shareGrantRepository = shareGrantRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ShareGrantResponse grantProjectAccess(Long projectId, ShareGrantRequest requestDto, Long granterUserId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Verify the granter is the owner of the project
        if (!project.getPlatformUserId().equals(granterUserId)) {
            throw new AccessDeniedException("Only the project owner can grant access");
        }

        // Create and save the share grant
        ShareGrant shareGrant = new ShareGrant();
        shareGrant.setProject(project);
        shareGrant.setGranteeUserId(requestDto.getGranteeUserId());
        shareGrant.setPermissionLevel(requestDto.getPermissionLevel());
        shareGrant.setGranterUserId(granterUserId);

        ShareGrant savedGrant = shareGrantRepository.save(shareGrant);

        // Map to response
        return new ShareGrantResponse(
            savedGrant.getId(),
            savedGrant.getProject().getId(),
            savedGrant.getGranteeUserId(),
            savedGrant.getGranterUserId(),
            savedGrant.getPermissionLevel()
        );
    }

    @Transactional
    public void revokeProjectAccess(Long projectId, Long granteeUserId, Long revokerUserId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Verify the revoker is the owner of the project
        if (!project.getPlatformUserId().equals(revokerUserId)) {
            throw new AccessDeniedException("Only the project owner can revoke access");
        }

        // Find and delete the share grant
        ShareGrant shareGrant = shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, granteeUserId)
            .orElseThrow(() -> new ResourceNotFoundException("ShareGrant", "projectId and granteeUserId", 
                projectId + " and " + granteeUserId));

        shareGrantRepository.delete(shareGrant);
    }

    /**
     * Gets the effective permission level for a user on a project.
     * If the user is the owner, they have OWNER permission.
     * Otherwise, returns the granted permission level or null if no access.
     *
     * @param projectId the project ID
     * @param platformUserId the user ID
     * @return the effective permission level, or null if no access
     */
    public SharePermissionLevel getEffectivePermission(Long projectId, Long platformUserId) {
        // Check if user is the owner
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return null;
        }

        if (project.getPlatformUserId().equals(platformUserId)) {
            return SharePermissionLevel.OWNER;
        }

        // Check for shared access
        return shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, platformUserId)
            .map(ShareGrant::getPermissionLevel)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listSharedProjectsForUser(Long platformUserId) {
        return shareGrantRepository.findByGranteeUserId(platformUserId).stream()
            .map(grant -> projectMapper.toProjectResponse(grant.getProject()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShareGrantResponse> listUsersForProject(Long projectId, Long platformUserId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Verify the requester has at least view access
        SharePermissionLevel permission = getEffectivePermission(projectId, platformUserId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have access to this project");
        }

        return shareGrantRepository.findByProjectId(projectId).stream()
            .map(grant -> new ShareGrantResponse(
                grant.getId(),
                grant.getProject().getId(),
                grant.getGranteeUserId(),
                grant.getGranterUserId(),
                grant.getPermissionLevel()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllGrantsForProject(Long projectId) {
        shareGrantRepository.deleteByProjectId(projectId);
    }
}

