package com.codebridge.gitlab.git.dto;

import com.codebridge.gitlab.git.model.GitProvider.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitProviderDto {
    
    private UUID id;
    
    @NotBlank(message = "Provider name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Provider type is required")
    private ProviderType type;
    
    @NotBlank(message = "Base URL is required")
    private String baseUrl;
    
    @NotBlank(message = "API URL is required")
    private String apiUrl;
    
    private String iconUrl;
    
    private String documentationUrl;
    
    private boolean enabled;
    
    private boolean supportsOAuth;
    
    private boolean supportsPAT;
    
    private boolean supportsSSH;
    
    private boolean supportsWebhooks;
    
    private String oauthClientId;
    
    private String oauthClientSecret;
    
    private String oauthRedirectUri;
    
    private String oauthScopes;
    
    private String apiVersion;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

