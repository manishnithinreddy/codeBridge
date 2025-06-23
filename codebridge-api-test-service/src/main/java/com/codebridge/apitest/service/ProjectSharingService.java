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
     * Verify if a user has access to a project with the required permission level.
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
        ShareGrant grant = shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("User does not have access to this project"));

        if (grant.getPermissionLevel().ordinal() < requiredPermission.ordinal()) {
            throw new AccessDeniedException("User does not have sufficient permissions for this operation");
        }
    }

    /**
     * Verify if a user has any access to a project.
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
        shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("User does not have access to this project"));
    }

    /**
     * Get the effective permission level for a user on a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the effective permission level
     * @throws ResourceNotFoundException if the project is not found
     */
    public SharePermissionLevel getEffectivePermission(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Project owner has all permissions
        if (project.getUserId().equals(userId)) {
            return SharePermissionLevel.CAN_EDIT;
        }

        // Check if user has a share grant
        return shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, userId)
                .map(ShareGrant::getPermissionLevel)
                .orElse(SharePermissionLevel.NO_ACCESS);
    }

    /**
     * List all projects shared with a user.
     *
     * @param userId the user ID
     * @return the list of shared projects
     */
    public List<ProjectResponse> listSharedProjectsForUser(Long userId) {
        List<ShareGrant> grants = shareGrantRepository.findByGranteeUserId(userId);
        
        return grants.stream()
                .map(grant -> projectRepository.findById(grant.getProject().getId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    /**
     * List all shares for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of share grant responses
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have access
     */
    public List<ShareGrantResponse> listSharesForProject(Long projectId, Long userId) {
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
     * Create a share grant for a project.
     *
     * @param projectId the project ID
     * @param request the share grant request
     * @param granterUserId the user ID of the granter
     * @return the created share grant response
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have access
     * @throws IllegalArgumentException if the share already exists
     */
    @Transactional
    public ShareGrantResponse createShare(Long projectId, ShareGrantRequest request, Long granterUserId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can create shares
        if (!project.getUserId().equals(granterUserId)) {
            throw new AccessDeniedException("Only the project owner can create shares");
        }

        // Check if share already exists
        shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, request.getUserId())
                .ifPresent(grant -> {
                    throw new IllegalArgumentException("Share already exists for this user");
                });

        // Create share grant
        ShareGrant grant = new ShareGrant();
        grant.setProject(project);
        grant.setGranteeUserId(request.getUserId());
        grant.setPermissionLevel(request.getPermissionLevel());
        grant.setGranterUserId(granterUserId);

        ShareGrant savedGrant = shareGrantRepository.save(grant);
        return mapToShareGrantResponse(savedGrant);
    }
    
    /**
     * Revoke a share grant for a project.
     *
     * @param projectId the project ID
     * @param granteeUserId the user ID of the grantee
     * @param revokerUserId the user ID of the revoker
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have access
     */
    @Transactional
    public void revokeShare(Long projectId, Long granteeUserId, Long revokerUserId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can revoke shares
        if (!project.getUserId().equals(revokerUserId)) {
            throw new AccessDeniedException("Only the project owner can revoke shares");
        }

        // Find the share grant
        ShareGrant grant = shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, granteeUserId)
                .orElseThrow(() -> new ResourceNotFoundException("ShareGrant", "granteeUserId", granteeUserId.toString()));

        // Delete the share grant
        shareGrantRepository.delete(grant);
    }

    /**
     * List all shares for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of share grant responses
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have access
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
     * Create a share grant for a project.
     *
     * @param projectId the project ID
     * @param request the share grant request
     * @param userId the user ID
     * @return the created share grant response
     * @throws ResourceNotFoundException if the project is not found
     * @throws AccessDeniedException if the user does not have access
     * @throws IllegalArgumentException if the share already exists
     */
    @Transactional
    public ShareGrantResponse createProjectShare(Long projectId, ShareGrantRequest request, Long userId) {
        // Verify user has access to the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));

        // Only project owner can create shares
        if (!project.getUserId().equals(userId)) {
            throw new AccessDeniedException("Only the project owner can create shares");
        }

        // Check if share already exists
        shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, request.getUserId())
                .ifPresent(grant -> {
                    throw new IllegalArgumentException("Share already exists for this user");
                });

        // Create share grant
        ShareGrant grant = new ShareGrant();
        grant.setProject(project);
        grant.setGranteeUserId(request.getUserId());
        grant.setPermissionLevel(request.getPermissionLevel());
        grant.setGranterUserId(userId);

        ShareGrant savedGrant = shareGrantRepository.save(grant);
        return mapToShareGrantResponse(savedGrant);
    }

    /**
     * Update a share grant for a project.
     *
     * @param projectId the project ID
     * @param shareId the share ID
     * @param request the share grant request
     * @param userId the user ID
     * @return the updated share grant response
     * @throws ResourceNotFoundException if the project or share is not found
     * @throws AccessDeniedException if the user does not have access
     */
    @Transactional
    public ShareGrantResponse updateProjectShare(Long projectId, Long shareId, ShareGrantRequest request, Long userId) {
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
        if (!grant.getProject().getId().equals(projectId)) {
            throw new AccessDeniedException("Share grant does not belong to this project");
        }

        // Update share grant
        grant.setPermissionLevel(request.getPermissionLevel());

        ShareGrant updatedGrant = shareGrantRepository.save(grant);
        return mapToShareGrantResponse(updatedGrant);
    }

    /**
     * Delete a share grant for a project.
     *
     * @param projectId the project ID
     * @param shareId the share ID
     * @param userId the user ID
     * @throws ResourceNotFoundException if the project or share is not found
     * @throws AccessDeniedException if the user does not have access
     */
    @Transactional
    public void deleteProjectShare(Long projectId, Long shareId, Long userId) {
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
        if (!grant.getProject().getId().equals(projectId)) {
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
        response.setProjectId(grant.getProject().getId());
        response.setUserId(grant.getGranteeUserId());
        response.setPermissionLevel(grant.getPermissionLevel());
        response.setCreatedAt(grant.getCreatedAt());
        response.setCreatedBy(grant.getGranterUserId());
        return response;
    }
    
    /**
     * Map a project to a response DTO.
     *
     * @param project the project
     * @return the project response
     */
    private ProjectResponse mapToProjectResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setUserId(project.getUserId());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}

