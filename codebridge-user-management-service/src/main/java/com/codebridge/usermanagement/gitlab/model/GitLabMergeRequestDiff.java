package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model class representing a GitLab Merge Request Diff.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabMergeRequestDiff {

    private Integer id;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("base_commit_sha")
    private String baseCommitSha;
    
    @JsonProperty("head_commit_sha")
    private String headCommitSha;
    
    @JsonProperty("start_commit_sha")
    private String startCommitSha;
    
    private List<GitLabDiffChange> changes;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBaseCommitSha() {
        return baseCommitSha;
    }

    public void setBaseCommitSha(String baseCommitSha) {
        this.baseCommitSha = baseCommitSha;
    }

    public String getHeadCommitSha() {
        return headCommitSha;
    }

    public void setHeadCommitSha(String headCommitSha) {
        this.headCommitSha = headCommitSha;
    }

    public String getStartCommitSha() {
        return startCommitSha;
    }

    public void setStartCommitSha(String startCommitSha) {
        this.startCommitSha = startCommitSha;
    }

    public List<GitLabDiffChange> getChanges() {
        return changes;
    }

    public void setChanges(List<GitLabDiffChange> changes) {
        this.changes = changes;
    }
}

