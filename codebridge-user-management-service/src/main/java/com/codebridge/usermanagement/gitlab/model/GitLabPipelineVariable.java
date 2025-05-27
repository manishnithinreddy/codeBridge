package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model class representing a GitLab Pipeline Variable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabPipelineVariable {

    private String key;
    private String value;
    private Boolean variable_type;

    // Getters and setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getVariable_type() {
        return variable_type;
    }

    public void setVariable_type(Boolean variable_type) {
        this.variable_type = variable_type;
    }
}

