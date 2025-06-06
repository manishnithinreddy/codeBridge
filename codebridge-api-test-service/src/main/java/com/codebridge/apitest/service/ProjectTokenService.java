package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ProjectTokenRequest;
import com.codebridge.apitest.dto.ProjectTokenResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ProjectToken;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.repository.ProjectRepository;
import com.codebridge.apitest.repository.ProjectTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing project tokens.
 */
@Service
public class ProjectTokenService {

    private final ProjectTokenRepository projectTokenRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSharingService projectSharingService;

    @Autowired
    public ProjectTokenService(ProjectTokenRepository projectTokenRepository,
                              ProjectRepository projectRepository,
                              ProjectSharingService projectSharingService) {
        this.projectTokenRepository = projectTokenRepository;
        this.projectRepository = projectRepository;
        this.projectSharingService = projectSharingService;
    }

    /**
     * Create a new project token.
     *
     * @param projectId the project ID
     * @param request the token request
     * @param userId the user ID
     * @return the created token
     */
    @Transactional
    public ProjectTokenResponse createToken(UUID projectId, ProjectTokenRequest request, UUID userId) {
        // Check if project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId.toString());
        }

        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to create tokens for project " + projectId);
        }

        // Create token
        ProjectToken token = new ProjectToken();
        token.setId(UUID.randomUUID());
        token.setProjectId(projectId);
        token.setName(request.getName());
        token.setTokenType(request.getTokenType());
        token.setTokenValue(request.getTokenValue());
        token.setHeaderName(request.getHeaderName());
        token.setParameterName(request.getParameterName());
        token.setTokenLocation(request.getTokenLocation() != null ? request.getTokenLocation() : "header");
        token.setExpiresAt(request.getExpiresAt());
        token.setRefreshUrl(request.getRefreshUrl());
        token.setRefreshData(request.getRefreshData());
        token.setActive(request.getActive());
        token.setAutoRefresh(request.getAutoRefresh() != null ? request.getAutoRefresh() : false);
        token.setCreatedBy(userId);

        ProjectToken savedToken = projectTokenRepository.save(token);
        return mapToResponse(savedToken);
    }

    /**
     * Get all tokens for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of tokens
     */
    @Transactional(readOnly = true)
    public List<ProjectTokenResponse> getTokens(UUID projectId, UUID userId) {
        // Check if project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId.toString());
        }

        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to view tokens for project " + projectId);
        }

        // Get tokens
        List<ProjectToken> tokens = projectTokenRepository.findByProjectId(projectId);
        return tokens.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a token by ID.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param userId the user ID
     * @return the token
     */
    @Transactional(readOnly = true)
    public ProjectTokenResponse getToken(UUID projectId, UUID tokenId, UUID userId) {
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to view tokens for project " + projectId);
        }

        // Get token
        ProjectToken token = projectTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId.toString()));

        // Verify token belongs to project
        if (!token.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Token does not belong to project " + projectId);
        }

        return mapToResponse(token);
    }

    /**
     * Update a token.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param request the token request
     * @param userId the user ID
     * @return the updated token
     */
    @Transactional
    public ProjectTokenResponse updateToken(UUID projectId, UUID tokenId, ProjectTokenRequest request, UUID userId) {
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to update tokens for project " + projectId);
        }

        // Get token
        ProjectToken token = projectTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId.toString()));

        // Verify token belongs to project
        if (!token.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Token does not belong to project " + projectId);
        }

        // Update token
        token.setName(request.getName());
        token.setTokenType(request.getTokenType());
        token.setTokenValue(request.getTokenValue());
        token.setHeaderName(request.getHeaderName());
        token.setParameterName(request.getParameterName());
        token.setTokenLocation(request.getTokenLocation() != null ? request.getTokenLocation() : token.getTokenLocation());
        token.setExpiresAt(request.getExpiresAt());
        token.setRefreshUrl(request.getRefreshUrl());
        token.setRefreshData(request.getRefreshData());
        token.setActive(request.getActive());
        token.setAutoRefresh(request.getAutoRefresh() != null ? request.getAutoRefresh() : token.isAutoRefresh());
        token.setUpdatedAt(LocalDateTime.now());

        ProjectToken savedToken = projectTokenRepository.save(token);
        return mapToResponse(savedToken);
    }

    /**
     * Delete a token.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteToken(UUID projectId, UUID tokenId, UUID userId) {
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to delete tokens for project " + projectId);
        }

        // Get token
        ProjectToken token = projectTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId.toString()));

        // Verify token belongs to project
        if (!token.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Token does not belong to project " + projectId);
        }

        // Delete token
        projectTokenRepository.delete(token);
    }

    /**
     * Get active tokens for a project.
     *
     * @param projectId the project ID
     * @return the list of active tokens
     */
    @Transactional(readOnly = true)
    public List<ProjectToken> getActiveTokens(UUID projectId) {
        return projectTokenRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Map a token entity to a response DTO.
     *
     * @param token the token entity
     * @return the token response
     */
    private ProjectTokenResponse mapToResponse(ProjectToken token) {
        ProjectTokenResponse response = new ProjectTokenResponse();
        response.setId(token.getId());
        response.setProjectId(token.getProjectId());
        response.setName(token.getName());
        response.setTokenType(token.getTokenType());
        response.setTokenValue(token.getTokenValue());
        response.setHeaderName(token.getHeaderName());
        response.setParameterName(token.getParameterName());
        response.setTokenLocation(token.getTokenLocation());
        response.setExpiresAt(token.getExpiresAt());
        response.setRefreshUrl(token.getRefreshUrl());
        response.setActive(token.isActive());
        response.setAutoRefresh(token.isAutoRefresh());
        response.setCreatedAt(token.getCreatedAt());
        response.setUpdatedAt(token.getUpdatedAt());
        response.setCreatedBy(token.getCreatedBy());
        return response;
    }
}

