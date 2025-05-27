package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class representing a position in a file for GitLab inline comments.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabPosition {

    @JsonProperty("base_sha")
    private String baseSha;
    
    @JsonProperty("head_sha")
    private String headSha;
    
    @JsonProperty("start_sha")
    private String startSha;
    
    @JsonProperty("old_path")
    private String oldPath;
    
    @JsonProperty("new_path")
    private String newPath;
    
    @JsonProperty("old_line")
    private Integer oldLine;
    
    @JsonProperty("new_line")
    private Integer newLine;
    
    @JsonProperty("position_type")
    private String positionType;  // "text" or "image"

    // Getters and setters
    public String getBaseSha() {
        return baseSha;
    }

    public void setBaseSha(String baseSha) {
        this.baseSha = baseSha;
    }

    public String getHeadSha() {
        return headSha;
    }

    public void setHeadSha(String headSha) {
        this.headSha = headSha;
    }

    public String getStartSha() {
        return startSha;
    }

    public void setStartSha(String startSha) {
        this.startSha = startSha;
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public Integer getOldLine() {
        return oldLine;
    }

    public void setOldLine(Integer oldLine) {
        this.oldLine = oldLine;
    }

    public Integer getNewLine() {
        return newLine;
    }

    public void setNewLine(Integer newLine) {
        this.newLine = newLine;
    }

    public String getPositionType() {
        return positionType;
    }

    public void setPositionType(String positionType) {
        this.positionType = positionType;
    }
}

