package com.codebridge.gitlab.git.dto;

import com.codebridge.gitlab.git.model.Repository.VisibilityType;
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
public class RepositoryDto {
    
    private UUID id;
    
    @NotBlank(message = "Repository name is required")
    private String name;
    
    private String displayName;
    
    private String description;
    
    @NotBlank(message = "Remote ID is required")
    private String remoteId;
    
    @NotBlank(message = "Remote URL is required")
    private String remoteUrl;
    
    @NotBlank(message = "Clone URL is required")
    private String cloneUrl;
    
    private String sshUrl;
    
    @NotBlank(message = "Web URL is required")
    private String webUrl;
    
    @NotBlank(message = "Default branch is required")
    private String defaultBranch;
    
    @NotNull(message = "Visibility is required")
    private VisibilityType visibility;
    
    private UUID teamId;
    
    private String ownerName;
    
    private String avatarUrl;
    
    private Integer forkCount;
    
    private Integer starCount;
    
    private Integer watchCount;
    
    private boolean fork;
    
    private boolean archived;
    
    private boolean template;
    
    private LocalDateTime lastSyncedAt;
    
    @NotNull(message = "Provider ID is required")
    private UUID providerId;
    
    private String providerName;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

