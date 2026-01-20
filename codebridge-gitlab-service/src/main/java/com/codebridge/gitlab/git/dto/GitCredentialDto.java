package com.codebridge.gitlab.git.dto;

import com.codebridge.gitlab.git.model.GitCredential.CredentialType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GitCredentialDto {
    
    private UUID id;
    
    private UUID userId;
    
    private UUID teamId;
    
    @NotBlank(message = "Credential name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Credential type is required")
    private CredentialType type;
    
    private String username;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String token;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sshPrivateKey;
    
    private String sshPublicKey;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sshPassphrase;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime lastUsedAt;
    
    private boolean isDefault;
    
    @NotNull(message = "Provider ID is required")
    private UUID providerId;
    
    private String providerName;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

