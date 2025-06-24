package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;
import com.codebridge.docker.service.DockerAuthService;
import io.jsonwebtoken.Claims;
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
import java.util.function.Function;

@Slf4j
@Service
public class DockerAuthServiceImpl implements DockerAuthService {

    private final Key secretKey;
    
    @Value("${docker.auth.token-expiration}")
    private long tokenExpiration;

    public DockerAuthServiceImpl() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.info("Authenticating user: {}", authRequest.getUsername());
        
        // For demonstration purposes, we'll simulate authentication
        // In a real implementation, we would validate with Docker Registry
        
        // Simple validation for demo purposes
        if ("admin".equals(authRequest.getUsername()) && "admin".equals(authRequest.getPassword())) {
            String token = generateToken(authRequest.getUsername());
            
            return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(tokenExpiration)
                .username(authRequest.getUsername())
                .build();
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
    
    private String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        
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

