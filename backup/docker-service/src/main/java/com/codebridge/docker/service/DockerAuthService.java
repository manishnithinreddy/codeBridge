package com.codebridge.docker.service;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;

public interface DockerAuthService {
    
    /**
     * Authenticate a user with Docker Registry
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

