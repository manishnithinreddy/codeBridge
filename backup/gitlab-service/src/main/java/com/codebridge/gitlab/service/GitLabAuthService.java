package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.AuthRequest;
import com.codebridge.gitlab.model.AuthResponse;

public interface GitLabAuthService {
    
    /**
     * Authenticate a user with GitLab using username/password or personal access token
     * 
     * @param authRequest The authentication request containing credentials
     * @return AuthResponse with JWT token for subsequent API calls
     */
    AuthResponse authenticate(AuthRequest authRequest);
    
    /**
     * Validate a JWT token
     * 
     * @param token The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    boolean validateToken(String token);
}

