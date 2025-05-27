package com.codebridge.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    
    private UUID userId;
    private String username;
    private UUID currentTeamId;
    private String currentTeamName;
    private boolean isPersonalContext;
    
    public static UserContext createPersonalContext(UUID userId, String username) {
        return UserContext.builder()
                .userId(userId)
                .username(username)
                .isPersonalContext(true)
                .build();
    }
    
    public static UserContext createTeamContext(UUID userId, String username, UUID teamId, String teamName) {
        return UserContext.builder()
                .userId(userId)
                .username(username)
                .currentTeamId(teamId)
                .currentTeamName(teamName)
                .isPersonalContext(false)
                .build();
    }
}

