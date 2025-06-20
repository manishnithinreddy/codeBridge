package com.codebridge.apitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // Added for @NotNull

import java.util.Map;

/**
 * Request DTO for collection operations.
 */
public class CollectionRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;

    @NotNull(message = "Project ID cannot be null") // Added projectId
    private Long projectId;                         // Changed from UUID to Long

    private Map<String, String> variables;

    private String preRequestScript;

    private String postRequestScript;

    private boolean shared;

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

    // Getter and Setter for projectId
    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
