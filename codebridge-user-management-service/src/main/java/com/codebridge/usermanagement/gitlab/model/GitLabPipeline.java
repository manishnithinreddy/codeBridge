package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model class representing a GitLab Pipeline.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabPipeline {

    private Integer id;
    private String status;
    private String ref;
    private String sha;
    
    @JsonProperty("web_url")
    private String webUrl;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    
    @JsonProperty("finished_at")
    private LocalDateTime finishedAt;
    
    @JsonProperty("duration")
    private Integer duration;
    
    @JsonProperty("queued_duration")
    private Integer queuedDuration;
    
    private GitLabUser user;
    
    private List<GitLabPipelineVariable> variables;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getQueuedDuration() {
        return queuedDuration;
    }

    public void setQueuedDuration(Integer queuedDuration) {
        this.queuedDuration = queuedDuration;
    }

    public GitLabUser getUser() {
        return user;
    }

    public void setUser(GitLabUser user) {
        this.user = user;
    }

    public List<GitLabPipelineVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<GitLabPipelineVariable> variables) {
        this.variables = variables;
    }
}

