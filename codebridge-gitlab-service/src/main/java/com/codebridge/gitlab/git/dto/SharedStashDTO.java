package com.codebridge.gitlab.git.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for representing a shared stash in API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStashDTO {

    /**
     * The ID of the shared stash.
     */
    private UUID id;

    /**
     * The Git hash of the stash commit.
     */
    private String stashHash;

    /**
     * The ID of the repository that this stash belongs to.
     */
    private UUID repositoryId;

    /**
     * The name of the repository that this stash belongs to.
     */
    private String repositoryName;

    /**
     * The user who shared this stash.
     */
    private String sharedBy;

    /**
     * The timestamp when this stash was shared.
     */
    private LocalDateTime sharedAt;

    /**
     * A description of the stash provided by the user.
     */
    private String description;

    /**
     * The branch that the stash was created from.
     */
    private String branch;
    
    // Manual getters and setters in case Lombok is not working
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
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
    
    public String getRepositoryName() {
        return repositoryName;
    }
    
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
    
    public String getSharedBy() {
        return sharedBy;
    }
    
    public void setSharedBy(String sharedBy) {
        this.sharedBy = sharedBy;
    }
    
    public LocalDateTime getSharedAt() {
        return sharedAt;
    }
    
    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
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
