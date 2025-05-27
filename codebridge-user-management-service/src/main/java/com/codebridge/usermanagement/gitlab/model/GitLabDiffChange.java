package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class representing a change in a GitLab Merge Request Diff.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabDiffChange {

    @JsonProperty("old_path")
    private String oldPath;
    
    @JsonProperty("new_path")
    private String newPath;
    
    @JsonProperty("a_mode")
    private String aMode;
    
    @JsonProperty("b_mode")
    private String bMode;
    
    private String diff;
    
    @JsonProperty("new_file")
    private Boolean newFile;
    
    @JsonProperty("renamed_file")
    private Boolean renamedFile;
    
    @JsonProperty("deleted_file")
    private Boolean deletedFile;

    // Getters and setters
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

    public String getAMode() {
        return aMode;
    }

    public void setAMode(String aMode) {
        this.aMode = aMode;
    }

    public String getBMode() {
        return bMode;
    }

    public void setBMode(String bMode) {
        this.bMode = bMode;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public Boolean getNewFile() {
        return newFile;
    }

    public void setNewFile(Boolean newFile) {
        this.newFile = newFile;
    }

    public Boolean getRenamedFile() {
        return renamedFile;
    }

    public void setRenamedFile(Boolean renamedFile) {
        this.renamedFile = renamedFile;
    }

    public Boolean getDeletedFile() {
        return deletedFile;
    }

    public void setDeletedFile(Boolean deletedFile) {
        this.deletedFile = deletedFile;
    }
}

