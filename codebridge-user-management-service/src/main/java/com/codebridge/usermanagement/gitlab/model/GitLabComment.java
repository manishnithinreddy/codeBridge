package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Model class representing a GitLab Comment (Note).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabComment {

    private Integer id;
    private String body;
    private GitLabUser author;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("system")
    private Boolean isSystem;
    
    @JsonProperty("resolvable")
    private Boolean isResolvable;
    
    @JsonProperty("resolved")
    private Boolean isResolved;
    
    @JsonProperty("resolved_by")
    private GitLabUser resolvedBy;
    
    // For inline comments
    private String path;
    
    @JsonProperty("line")
    private Integer lineNumber;
    
    @JsonProperty("line_type")
    private String lineType;  // "new" or "old"
    
    @JsonProperty("position")
    private GitLabPosition position;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public GitLabUser getAuthor() {
        return author;
    }

    public void setAuthor(GitLabUser author) {
        this.author = author;
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

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Boolean getIsResolvable() {
        return isResolvable;
    }

    public void setIsResolvable(Boolean isResolvable) {
        this.isResolvable = isResolvable;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }

    public GitLabUser getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(GitLabUser resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public GitLabPosition getPosition() {
        return position;
    }

    public void setPosition(GitLabPosition position) {
        this.position = position;
    }
}

