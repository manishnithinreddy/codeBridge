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
    public ProjectTokenService(
            ProjectTokenRepository projectTokenRepository,
            ProjectRepository projectRepository,
            ProjectSharingService projectSharingService) {
        this.projectTokenRepository = projectTokenRepository;
        this.projectRepository = projectRepository;
        this.projectSharingService = projectSharingService;
    }

    /**
     * Get all tokens for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return list of token responses
     */
    public List<ProjectTokenResponse> getProjectTokens(Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId);
        
        return projectTokenRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new project token.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @param request the token request
     * @return the created token response
     */
    @Transactional
    public ProjectTokenResponse createProjectToken(Long projectId, Long userId, ProjectTokenRequest request) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.ADMIN);
        
        // Check if a token with the same name already exists
        projectTokenRepository.findByProjectIdAndName(projectId, request.getName())
                .ifPresent(token -> {
                    throw new IllegalArgumentException("A token with this name already exists");
                });
        
        ProjectToken token = new ProjectToken();
        token.setProjectId(projectId);
        token.setName(request.getName());
        token.setDescription(request.getDescription());
        token.setCreatedBy(userId);
        token.setExpiresAt(request.getExpiresAt());
        token.setActive(true);
        
        // Generate a secure token value
        String tokenValue = generateSecureToken();
        token.setTokenValue(tokenValue);
        
        ProjectToken savedToken = projectTokenRepository.save(token);
        
        // Return the response with the clear text token value (only time it's exposed)
        ProjectTokenResponse response = mapToResponse(savedToken);
        response.setTokenValue(tokenValue);
        return response;
    }

    /**
     * Revoke a project token.
     *
     * @param projectId the project ID
     * @param tokenId the token ID
     * @param userId the user ID
     */
    @Transactional
    public void revokeProjectToken(Long projectId, Long tokenId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.ADMIN);
        
        ProjectToken token = projectTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        
        if (!token.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Token does not belong to this project");
        }
        
        token.setActive(false);
        token.setRevokedAt(LocalDateTime.now());
        token.setRevokedBy(userId);
        
        projectTokenRepository.save(token);
    }

    /**
     * Validate a token for API access.
     *
     * @param tokenValue the token value
     * @return the project ID if valid
     * @throws AccessDeniedException if the token is invalid
     */
    public Long validateToken(String tokenValue) {
        // Implementation would need to be updated to search by token value
        // This is a placeholder for the actual implementation
        throw new AccessDeniedException("Invalid token");
    }

    /**
     * Generate a secure random token.
     *
     * @return a secure token string
     */
    private String generateSecureToken() {
        // Implementation would generate a secure random token
        // This is a placeholder for the actual implementation
        return "secure-token-" + System.currentTimeMillis();
    }

    /**
     * Map a token entity to a response DTO.
     *
     * @param token the token entity
     * @return the token response DTO
     */
    private ProjectTokenResponse mapToResponse(ProjectToken token) {
        ProjectTokenResponse response = new ProjectTokenResponse();
        response.setId(token.getId());
        response.setName(token.getName());
        response.setDescription(token.getDescription());
        response.setCreatedAt(token.getCreatedAt());
        response.setExpiresAt(token.getExpiresAt());
        response.setActive(token.getActive());
        response.setRevokedAt(token.getRevokedAt());
        // Token value is not included in the response for security reasons
        return response;
    }
}

