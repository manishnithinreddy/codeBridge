package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.AuthRequest;
import com.codebridge.gitlab.model.AuthResponse;

/**
 * Service for GitLab authentication operations.
 */
public interface GitLabAuthService {
    
    /**
     * Authenticates a user with GitLab using personal access token.
     *
     * @param authRequest The authentication request containing username and personal access token
     * @return AuthResponse containing JWT token for authenticated user
     */
    AuthResponse authenticate(AuthRequest authRequest);
}

