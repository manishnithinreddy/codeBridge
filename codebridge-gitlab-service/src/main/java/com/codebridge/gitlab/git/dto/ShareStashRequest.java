package com.codebridge.gitlab.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for requesting to share a stash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareStashRequest {

    /**
     * The Git hash of the stash commit.
     */
    @NotBlank(message = "Stash hash is required")
    private String stashHash;

    /**
     * The ID of the repository that this stash belongs to.
     */
    @NotNull(message = "Repository ID is required")
    private UUID repositoryId;

    /**
     * A description of the stash provided by the user.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * The branch that the stash was created from.
     */
    private String branch;
    
    // Manual getters and setters in case Lombok is not working
    public String getStashHash() {
        return stashHash;
    }
    
    public void setStashHash(String stashHash) {
        this.stashHash = stashHash;
    }
    
    public UUID getRepositoryId() {
        return repositoryId;
    }
    
    public void setRepositoryId(UUID repositoryId) {
        this.repositoryId = repositoryId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
}
