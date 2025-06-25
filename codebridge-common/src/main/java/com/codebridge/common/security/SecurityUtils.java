package com.codebridge.common.security;

import com.codebridge.common.model.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for security operations
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the current user context from the security context
     */
    public static UserContext getCurrentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            
            // Extract user information from JWT claims
            String userId = jwt.getClaimAsString("sub");
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            
            // Extract roles and permissions
            Set<String> roles = getRolesFromAuthentication(authentication);
            
            // Check if user is in team context
            UserContext.ContextType contextType = UserContext.ContextType.PERSONAL;
            UUID teamId = null;
            String teamName = null;
            
            Map<String, Object> teamContext = jwt.getClaimAsMap("team_context");
            if (teamContext != null && !teamContext.isEmpty()) {
                contextType = UserContext.ContextType.TEAM;
                teamId = UUID.fromString((String) teamContext.get("team_id"));
                teamName = (String) teamContext.get("team_name");
            }
            
            return UserContext.builder()
                    .userId(UUID.fromString(userId))
                    .username(username)
                    .email(email)
                    .contextType(contextType)
                    .teamId(teamId)
                    .teamName(teamName)
                    .roles(roles)
                    .permissions(getPermissionsFromRoles(roles))
                    .build();
        }
        
        return null;
    }

    /**
     * Get the current user ID from the security context
     */
    public static UUID getCurrentUserId() {
        UserContext userContext = getCurrentUserContext();
        return userContext != null ? userContext.getUserId() : null;
    }

    /**
     * Get the current username from the security context
     */
    public static String getCurrentUsername() {
        UserContext userContext = getCurrentUserContext();
        return userContext != null ? userContext.getUsername() : null;
    }

    /**
     * Check if the current user has the specified role
     */
    public static boolean hasRole(String role) {
        UserContext userContext = getCurrentUserContext();
        return userContext != null && userContext.hasRole(role);
    }

    /**
     * Check if the current user has the specified permission
     */
    public static boolean hasPermission(String permission) {
        UserContext userContext = getCurrentUserContext();
        return userContext != null && userContext.hasPermission(permission);
    }

    /**
     * Extract roles from the authentication object
     */
    private static Set<String> getRolesFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return Collections.emptySet();
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * Map roles to permissions (simplified implementation)
     * In a real application, this would be more sophisticated
     */
    private static Set<String> getPermissionsFromRoles(Set<String> roles) {
        // This is a simplified implementation
        // In a real application, you would have a more sophisticated mapping
        return roles.stream()
                .map(role -> {
                    if (role.startsWith("ROLE_")) {
                        role = role.substring(5).toLowerCase();
                    }
                    return role + ":read";
                })
                .collect(Collectors.toSet());
    }
}

