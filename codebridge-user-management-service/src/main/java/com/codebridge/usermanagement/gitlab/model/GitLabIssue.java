package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model class representing a GitLab Issue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabIssue {

    private Integer id;
    private Integer iid;
    private String title;
    private String description;
    private String state;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("closed_at")
    private LocalDateTime closedAt;
    
    @JsonProperty("web_url")
    private String webUrl;
    
    private List<String> labels;
    
    private GitLabUser author;
    
    private List<GitLabUser> assignees;
    
    @JsonProperty("milestone")
    private GitLabMilestone milestone;
    
    @JsonProperty("due_date")
    private String dueDate;
    
    @JsonProperty("confidential")
    private Boolean confidential;
    
    @JsonProperty("discussion_locked")
    private Boolean discussionLocked;
    
    @JsonProperty("user_notes_count")
    private Integer userNotesCount;
    
    @JsonProperty("upvotes")
    private Integer upvotes;
    
    @JsonProperty("downvotes")
    private Integer downvotes;
    
    @JsonProperty("time_stats")
    private GitLabTimeStats timeStats;

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

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public GitLabUser getAuthor() {
        return author;
    }

    public void setAuthor(GitLabUser author) {
        this.author = author;
    }

    public List<GitLabUser> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<GitLabUser> assignees) {
        this.assignees = assignees;
    }

    public GitLabMilestone getMilestone() {
        return milestone;
    }

    public void setMilestone(GitLabMilestone milestone) {
        this.milestone = milestone;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getConfidential() {
        return confidential;
    }

    public void setConfidential(Boolean confidential) {
        this.confidential = confidential;
    }

    public Boolean getDiscussionLocked() {
        return discussionLocked;
    }

    public void setDiscussionLocked(Boolean discussionLocked) {
        this.discussionLocked = discussionLocked;
    }

    public Integer getUserNotesCount() {
        return userNotesCount;
    }

    public void setUserNotesCount(Integer userNotesCount) {
        this.userNotesCount = userNotesCount;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public Integer getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(Integer downvotes) {
        this.downvotes = downvotes;
    }

    public GitLabTimeStats getTimeStats() {
        return timeStats;
    }

    public void setTimeStats(GitLabTimeStats timeStats) {
        this.timeStats = timeStats;
    }
}

