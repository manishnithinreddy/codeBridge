package com.codebridge.docker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a Docker Image.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerImage {

    private String id;
    
    @JsonProperty("registry_id")
    private String registryId;
    
    private String name;
    private String tag;
    
    @JsonProperty("full_name")
    private String fullName;
    
    @JsonProperty("digest")
    private String digest;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("pushed_at")
    private LocalDateTime pushedAt;
    
    @JsonProperty("pull_count")
    private Long pullCount;
    
    @JsonProperty("star_count")
    private Long starCount;
    
    private Long size;
    
    private Map<String, String> labels;
    
    private List<String> tags;
    
    private Map<String, Object> metadata;
    
    @JsonProperty("is_official")
    private boolean isOfficial;
    
    @JsonProperty("is_automated")
    private boolean isAutomated;
    
    private String description;
    
    @JsonProperty("dockerfile_path")
    private String dockerfilePath;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegistryId() {
        return registryId;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(LocalDateTime pushedAt) {
        this.pushedAt = pushedAt;
    }

    public Long getPullCount() {
        return pullCount;
    }

    public void setPullCount(Long pullCount) {
        this.pullCount = pullCount;
    }

    public Long getStarCount() {
        return starCount;
    }

    public void setStarCount(Long starCount) {
        this.starCount = starCount;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isOfficial() {
        return isOfficial;
    }

    public void setOfficial(boolean official) {
        isOfficial = official;
    }

    public boolean isAutomated() {
        return isAutomated;
    }

    public void setAutomated(boolean automated) {
        isAutomated = automated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDockerfilePath() {
        return dockerfilePath;
    }

    public void setDockerfilePath(String dockerfilePath) {
        this.dockerfilePath = dockerfilePath;
    }
}

