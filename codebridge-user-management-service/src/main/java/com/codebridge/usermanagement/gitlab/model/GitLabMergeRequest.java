package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model class representing a GitLab Merge Request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabMergeRequest {

    private Integer id;
    private Integer iid;
    private String title;
    private String description;
    private String state;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("target_branch")
    private String targetBranch;
    
    @JsonProperty("source_branch")
    private String sourceBranch;
    
    @JsonProperty("web_url")
    private String webUrl;
    
    @JsonProperty("merge_status")
    private String mergeStatus;
    
    @JsonProperty("user_notes_count")
    private Integer userNotesCount;
    
    @JsonProperty("approvals_required")
    private Integer approvalsRequired;
    
    @JsonProperty("approvals_left")
    private Integer approvalsLeft;
    
    private List<GitLabUser> approvers;
    
    private List<GitLabUser> assignees;
    
    private GitLabUser author;
    
    private Boolean draft;
    
    @JsonProperty("has_conflicts")
    private Boolean hasConflicts;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public Integer getUserNotesCount() {
        return userNotesCount;
    }

    public void setUserNotesCount(Integer userNotesCount) {
        this.userNotesCount = userNotesCount;
    }

    public Integer getApprovalsRequired() {
        return approvalsRequired;
    }

    public void setApprovalsRequired(Integer approvalsRequired) {
        this.approvalsRequired = approvalsRequired;
    }

    public Integer getApprovalsLeft() {
        return approvalsLeft;
    }

    public void setApprovalsLeft(Integer approvalsLeft) {
        this.approvalsLeft = approvalsLeft;
    }

    public List<GitLabUser> getApprovers() {
        return approvers;
    }

    public void setApprovers(List<GitLabUser> approvers) {
        this.approvers = approvers;
    }

    public List<GitLabUser> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<GitLabUser> assignees) {
        this.assignees = assignees;
    }

    public GitLabUser getAuthor() {
        return author;
    }

    public void setAuthor(GitLabUser author) {
        this.author = author;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

    public Boolean getHasConflicts() {
        return hasConflicts;
    }

    public void setHasConflicts(Boolean hasConflicts) {
        this.hasConflicts = hasConflicts;
    }
}

