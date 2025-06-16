package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;
import com.codebridge.docker.service.DockerAuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of DockerAuthService.
 */
@Slf4j
@Service
public class DockerAuthServiceImpl implements DockerAuthService {

    private final long tokenExpirationMs;
    private final Key signingKey;

    public DockerAuthServiceImpl(
            @Value("${docker.auth.token-expiration:86400000}") long tokenExpirationMs) {
        this.tokenExpirationMs = tokenExpirationMs;
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Authenticating user: {} for registry: {}", 
                authRequest.getUsername(), 
                authRequest.getRegistry() != null ? authRequest.getRegistry() : "default");
        
        // Validate Docker Registry credentials
        boolean isValidCredentials = validateCredentials(
                authRequest.getUsername(), 
                authRequest.getPassword(), 
                authRequest.getRegistry());
        
        if (!isValidCredentials) {
            log.error("Invalid Docker Registry credentials for user: {}", authRequest.getUsername());
            throw new RuntimeException("Invalid Docker Registry credentials");
        }
        
        // Generate JWT token
        String jwtToken = generateToken(authRequest.getUsername(), authRequest.getRegistry());
        
        // Create and return auth response
        return new AuthResponse(
                jwtToken,
                "Bearer",
                tokenExpirationMs / 1000,
                authRequest.getUsername()
        );
    }
    
    /**
     * Validates Docker Registry credentials.
     *
     * @param username Username
     * @param password Password
     * @param registry Registry URL
     * @return true if credentials are valid, false otherwise
     */
    private boolean validateCredentials(String username, String password, String registry) {
        // In a real implementation, this would validate against Docker Registry
        // For now, we'll just return true for demonstration purposes
        log.info("Validating Docker Registry credentials for user: {}", username);
        return true;
    }
    
    /**
     * Generates a JWT token for the authenticated user.
     *
     * @param username Username of the authenticated user
     * @param registry Registry URL
     * @return JWT token
     */
    private String generateToken(String username, String registry) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        if (registry != null && !registry.isEmpty()) {
            claims.put("registry", registry);
        }
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpirationMs);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }
}

