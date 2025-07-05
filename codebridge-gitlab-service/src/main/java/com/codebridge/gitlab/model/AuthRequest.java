package com.codebridge.gitlab.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an authentication request for GitLab.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Personal access token is required")
    private String personalAccessToken;
    
    // Manual getters in case Lombok is not working
    public String getUsername() {
        return username;
    }
    
    public String getPersonalAccessToken() {
        return personalAccessToken;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPersonalAccessToken(String personalAccessToken) {
        this.personalAccessToken = personalAccessToken;
    }
}
