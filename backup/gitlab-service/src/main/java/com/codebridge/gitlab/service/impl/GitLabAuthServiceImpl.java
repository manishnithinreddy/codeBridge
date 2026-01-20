package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.config.GitLabApiConfig;
import com.codebridge.gitlab.model.AuthRequest;
import com.codebridge.gitlab.model.AuthResponse;
import com.codebridge.gitlab.service.GitLabAuthService;
import io.jsonwebtoken.Claims;
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
import java.util.function.Function;

@Slf4j
@Service
public class GitLabAuthServiceImpl implements GitLabAuthService {

    private final GitLabApiConfig gitLabApiConfig;
    private final RestTemplate restTemplate;
    private final Key secretKey;
    
    @Value("${gitlab.auth.token-expiration}")
    private long tokenExpiration;

    public GitLabAuthServiceImpl(GitLabApiConfig gitLabApiConfig) {
        this.gitLabApiConfig = gitLabApiConfig;
        this.restTemplate = new RestTemplate();
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Authenticating user: {}", authRequest.getUsername());
        
        // For demonstration purposes, we'll simulate authentication
        // In a real implementation, we would validate with GitLab API
        
        if (authRequest.getPersonalAccessToken() != null && !authRequest.getPersonalAccessToken().isEmpty()) {
            // Validate personal access token with GitLab
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("PRIVATE-TOKEN", authRequest.getPersonalAccessToken());
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    gitLabApiConfig.getBaseUrl() + "/user",
                    HttpMethod.GET,
                    entity,
                    Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> userInfo = response.getBody();
                    String userId = userInfo.get("id").toString();
                    String username = userInfo.get("username").toString();
                    
                    String token = generateToken(username, userId);
                    
                    return AuthResponse.builder()
                        .token(token)
                        .tokenType("Bearer")
                        .expiresIn(tokenExpiration)
                        .username(username)
                        .userId(userId)
                        .build();
                }
            } catch (Exception e) {
                log.error("Error authenticating with GitLab API", e);
                throw new RuntimeException("Authentication failed: " + e.getMessage());
            }
        } else {
            // Username/password authentication would require OAuth flow with GitLab
            // This is a simplified version for demonstration
            if ("admin".equals(authRequest.getUsername()) && "admin".equals(authRequest.getPassword())) {
                String token = generateToken(authRequest.getUsername(), "1");
                
                return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(tokenExpiration)
                    .username(authRequest.getUsername())
                    .userId("1")
                    .build();
            }
        }
        
        throw new RuntimeException("Authentication failed: Invalid credentials");
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token", e);
            return false;
        }
    }
    
    private String generateToken(String username, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
            .signWith(secretKey)
            .compact();
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }
}

