package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.model.AuthRequest;
import com.codebridge.gitlab.model.AuthResponse;
import com.codebridge.gitlab.service.GitLabAuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of GitLabAuthService.
 */
@Slf4j
@Service
public class GitLabAuthServiceImpl implements GitLabAuthService {

    private final RestTemplate restTemplate;
    private final String gitLabApiBaseUrl;
    private final long tokenExpirationMs;
    private final Key signingKey;

    public GitLabAuthServiceImpl(
            RestTemplate restTemplate,
            @Value("${gitlab.api.base-url}") String gitLabApiBaseUrl,
            @Value("${gitlab.auth.token-expiration}") long tokenExpirationMs) {
        this.restTemplate = restTemplate;
        this.gitLabApiBaseUrl = gitLabApiBaseUrl;
        this.tokenExpirationMs = tokenExpirationMs;
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Authenticating user: {}", authRequest.getUsername());
        
        // Validate GitLab personal access token
        boolean isValidToken = validatePersonalAccessToken(authRequest.getPersonalAccessToken());
        
        if (!isValidToken) {
            log.error("Invalid GitLab personal access token for user: {}", authRequest.getUsername());
            throw new RuntimeException("Invalid GitLab personal access token");
        }
        
        // Generate JWT token
        String jwtToken = generateToken(authRequest.getUsername());
        
        // Create and return auth response
        return new AuthResponse(
                jwtToken,
                "Bearer",
                tokenExpirationMs / 1000,
                authRequest.getUsername()
        );
    }
    
    /**
     * Validates a GitLab personal access token by making a request to the GitLab API.
     *
     * @param personalAccessToken GitLab personal access token
     * @return true if token is valid, false otherwise
     */
    private boolean validatePersonalAccessToken(String personalAccessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", personalAccessToken);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/user",
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error validating GitLab personal access token", e);
            return false;
        }
    }
    
    /**
     * Generates a JWT token for the authenticated user.
     *
     * @param username Username of the authenticated user
     * @return JWT token
     */
    private String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        
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

