package com.codebridge.apitest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for collection operations.
 */
public class CollectionResponse {

    private Long id;
    private String name;
    private String description;
    private Map<String, String> variables;
    private String preRequestScript;
    private String postRequestScript;
    private boolean shared;
    private List<CollectionTestResponse> tests;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String userName;
    private Long projectId;
    private String projectName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public String getPreRequestScript() {
        return preRequestScript;
    }

    public void setPreRequestScript(String preRequestScript) {
        this.preRequestScript = preRequestScript;
    }

    public String getPostRequestScript() {
        return postRequestScript;
    }

    public void setPostRequestScript(String postRequestScript) {
        this.postRequestScript = postRequestScript;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public List<CollectionTestResponse> getTests() {
        return tests;
    }

    public void setTests(List<CollectionTestResponse> tests) {
        this.tests = tests;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
