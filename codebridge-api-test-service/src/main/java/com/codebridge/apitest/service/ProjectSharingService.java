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

    public ProjectSharingService(ProjectRepository projectRepository, ShareGrantRepository shareGrantRepository) {
        this.projectRepository = projectRepository;
        this.shareGrantRepository = shareGrantRepository;
    }

    /**
     * Verify if a user has access to a project with the specified permission level.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @param requiredPermission the required permission level
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have the required permission
     */
    public void verifyProjectAccess(Long projectId, Long userId, SharePermissionLevel requiredPermission) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Project owner has all permissions
        if (project.getUserId().equals(userId)) {
            return;
        }

        // Check if user has a share grant with sufficient permissions
        ShareGrant grant = shareGrantRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this project"));

        SharePermissionLevel grantedPermission = grant.getPermissionLevel();

        // Check if granted permission is sufficient
        if (!hasPermission(grantedPermission, requiredPermission)) {
            throw new AccessDeniedException("You do not have sufficient permissions for this operation");
        }
    }

    /**
     * Verify if a user has access to a project (any permission level).
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have access
     */
    public void verifyProjectAccess(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Project owner has all permissions
        if (project.getUserId().equals(userId)) {
            return;
        }

        // Check if user has any share grant
        shareGrantRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this project"));
    }
    
    /**
     * Get the effective permission level for a user on a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the effective permission level, or null if no access
     */
    public SharePermissionLevel getEffectivePermission(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Project owner has all permissions
        if (project.getUserId().equals(userId)) {
            return SharePermissionLevel.CAN_EDIT;
        }

        // Check if user has a share grant
        return shareGrantRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ShareGrant::getPermissionLevel)
                .orElse(null);
    }

    /**
     * Check if a granted permission is sufficient for a required permission.
     *
     * @param grantedPermission the granted permission
     * @param requiredPermission the required permission
     * @return true if the granted permission is sufficient
     */
    private boolean hasPermission(SharePermissionLevel grantedPermission, SharePermissionLevel requiredPermission) {
        // Handle aliases
        if (grantedPermission == SharePermissionLevel.VIEW_ONLY) {
            grantedPermission = SharePermissionLevel.CAN_VIEW;
        }

        // Check permission hierarchy
        if (grantedPermission == SharePermissionLevel.CAN_EDIT) {
            return true; // CAN_EDIT includes all permissions
        } else if (grantedPermission == SharePermissionLevel.CAN_EXECUTE) {
            return requiredPermission == SharePermissionLevel.CAN_VIEW 
                || requiredPermission == SharePermissionLevel.CAN_EXECUTE;
        } else if (grantedPermission == SharePermissionLevel.CAN_VIEW) {
            return requiredPermission == SharePermissionLevel.CAN_VIEW;
        }

        return false;
    }

    /**
     * Get all share grants for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of share grants
     */
    public List<ShareGrantResponse> getProjectShares(Long projectId, Long userId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can view shares
        if (!project.getUserId().equals(userId)) {
            throw new AccessDeniedException("Only the project owner can view shares");
        }

        List<ShareGrant> grants = shareGrantRepository.findByProjectId(projectId);
        return grants.stream()
                .map(this::mapToShareGrantResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new share grant.
     *
     * @param projectId the project ID
     * @param request the share grant request
     * @param userId the user ID
     * @return the created share grant
     */
    @Transactional
    public ShareGrantResponse createShareGrant(Long projectId, ShareGrantRequest request, Long userId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can create shares
        if (!project.getUserId().equals(userId)) {
            throw new AccessDeniedException("Only the project owner can create shares");
        }

        // Check if share already exists
        shareGrantRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .ifPresent(grant -> {
                    throw new IllegalArgumentException("Share already exists for this user");
                });

        // Create share grant
        ShareGrant grant = new ShareGrant();
        grant.setProjectId(projectId);
        grant.setUserId(request.getUserId());
        grant.setPermissionLevel(request.getPermissionLevel());
        grant.setCreatedBy(userId);

        ShareGrant savedGrant = shareGrantRepository.save(grant);
        return mapToShareGrantResponse(savedGrant);
    }

    /**
     * Update a share grant.
     *
     * @param projectId the project ID
     * @param shareId the share ID
     * @param request the share grant request
     * @param userId the user ID
     * @return the updated share grant
     */
    @Transactional
    public ShareGrantResponse updateShareGrant(Long projectId, Long shareId, ShareGrantRequest request, Long userId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can update shares
        if (!project.getUserId().equals(userId)) {
            throw new AccessDeniedException("Only the project owner can update shares");
        }

        // Get share grant
        ShareGrant grant = shareGrantRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("ShareGrant", "id", shareId.toString()));

        // Verify share grant belongs to project
        if (!grant.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Share grant does not belong to this project");
        }

        // Update share grant
        grant.setPermissionLevel(request.getPermissionLevel());
        ShareGrant savedGrant = shareGrantRepository.save(grant);
        return mapToShareGrantResponse(savedGrant);
    }

    /**
     * Delete a share grant.
     *
     * @param projectId the project ID
     * @param shareId the share ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteShareGrant(Long projectId, Long shareId, Long userId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can delete shares
        if (!project.getUserId().equals(userId)) {
            throw new AccessDeniedException("Only the project owner can delete shares");
        }

        // Get share grant
        ShareGrant grant = shareGrantRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("ShareGrant", "id", shareId.toString()));

        // Verify share grant belongs to project
        if (!grant.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Share grant does not belong to this project");
        }

        // Delete share grant
        shareGrantRepository.delete(grant);
    }

    /**
     * Map a share grant to a response DTO.
     *
     * @param grant the share grant
     * @return the share grant response
     */
    private ShareGrantResponse mapToShareGrantResponse(ShareGrant grant) {
        ShareGrantResponse response = new ShareGrantResponse();
        response.setId(grant.getId());
        response.setProjectId(grant.getProjectId());
        response.setUserId(grant.getUserId());
        response.setPermissionLevel(grant.getPermissionLevel());
        response.setCreatedAt(grant.getCreatedAt());
        response.setCreatedBy(grant.getCreatedBy());
        return response;
    }
}

