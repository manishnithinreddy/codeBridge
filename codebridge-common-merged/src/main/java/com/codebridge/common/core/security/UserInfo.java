package com.codebridge.core.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data class to hold user information extracted from JWT token.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private String userId;
    private String username;
    private String email;
    private List<String> roles;
    private String activeTeamId;
}

