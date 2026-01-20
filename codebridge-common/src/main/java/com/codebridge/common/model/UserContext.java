package com.codebridge.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    
    public enum ContextType {
        PERSONAL,
        TEAM
    }
    
    private UUID userId;
    private String username;
    private String email;
    private UUID teamId;
    private String teamName;
    private ContextType contextType;
    
    @Builder.Default
    private Set<String> roles = new HashSet<>();
    
    @Builder.Default
    private Set<String> permissions = new HashSet<>();
    
    public static UserContext createPersonalContext(UUID userId, String username) {
        return UserContext.builder()
                .userId(userId)
                .username(username)
                .contextType(ContextType.PERSONAL)
                .build();
    }
    
    public static UserContext createTeamContext(UUID userId, String username, UUID teamId, String teamName) {
        return UserContext.builder()
                .userId(userId)
                .username(username)
                .teamId(teamId)
                .teamName(teamName)
                .contextType(ContextType.TEAM)
                .build();
    }
    
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    public boolean isPersonalContext() {
        return contextType == ContextType.PERSONAL;
    }
}

