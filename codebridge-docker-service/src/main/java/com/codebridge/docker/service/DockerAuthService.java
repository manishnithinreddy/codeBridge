package com.codebridge.docker.service;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;

/**
 * Service for Docker Registry authentication operations.
 */
public interface DockerAuthService {
    
    /**
     * Authenticates a user with Docker Registry.
     *
     * @param authRequest The authentication request containing username and password
     * @return AuthResponse containing JWT token for authenticated user
     */
    AuthResponse authenticate(AuthRequest authRequest);
}

