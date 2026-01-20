package com.codebridge.common.util;

import com.codebridge.common.model.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(authentication.getName());
    }

    public static Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication instanceof JwtAuthenticationToken)) {
            return Optional.empty();
        }
        
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthenticationToken.getToken();
        
        String subClaim = jwt.getSubject();
        if (subClaim == null) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(UUID.fromString(subClaim));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<UserContext> getCurrentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication instanceof JwtAuthenticationToken)) {
            return Optional.empty();
        }
        
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthenticationToken.getToken();
        
        String subClaim = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        if (subClaim == null || username == null) {
            return Optional.empty();
        }
        
        try {
            UUID userId = UUID.fromString(subClaim);
            
            // Check if there's a team context
            Map<String, Object> teamContext = jwt.getClaimAsMap("team_context");
            if (teamContext != null && !teamContext.isEmpty()) {
                String teamId = (String) teamContext.get("team_id");
                String teamName = (String) teamContext.get("team_name");
                
                if (teamId != null && teamName != null) {
                    return Optional.of(UserContext.createTeamContext(
                            userId, 
                            username, 
                            UUID.fromString(teamId), 
                            teamName
                    ));
                }
            }
            
            // Default to personal context
            return Optional.of(UserContext.createPersonalContext(userId, username));
            
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

