package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Model class representing a GitLab Pipeline Job.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabJob {

    private Integer id;
    private String name;
    private String stage;
    private String status;
    private String ref;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    
    @JsonProperty("finished_at")
    private LocalDateTime finishedAt;
    
    private String tag;
    
    @JsonProperty("web_url")
    private String webUrl;
    
    @JsonProperty("pipeline")
    private GitLabPipeline pipeline;
    
    @JsonProperty("allow_failure")
    private Boolean allowFailure;
    
    private GitLabUser user;
    
    @JsonProperty("runner")
    private GitLabRunner runner;
    
    @JsonProperty("artifacts_file")
    private GitLabArtifact artifactsFile;
    
    private String coverage;
    
    @JsonProperty("duration")
    private Float duration;
    
    @JsonProperty("queued_duration")
    private Float queuedDuration;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public GitLabPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(GitLabPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Boolean getAllowFailure() {
        return allowFailure;
    }

    public void setAllowFailure(Boolean allowFailure) {
        this.allowFailure = allowFailure;
    }

    public GitLabUser getUser() {
        return user;
    }

    public void setUser(GitLabUser user) {
        this.user = user;
    }

    public GitLabRunner getRunner() {
        return runner;
    }

    public void setRunner(GitLabRunner runner) {
        this.runner = runner;
    }

    public GitLabArtifact getArtifactsFile() {
        return artifactsFile;
    }

    public void setArtifactsFile(GitLabArtifact artifactsFile) {
        this.artifactsFile = artifactsFile;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public Float getDuration() {
        return duration;
    }

    public void setDuration(Float duration) {
        this.duration = duration;
    }

    public Float getQueuedDuration() {
        return queuedDuration;
    }

    public void setQueuedDuration(Float queuedDuration) {
        this.queuedDuration = queuedDuration;
    }
}

