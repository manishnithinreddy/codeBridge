package com.codebridge.usermanagement.gitlab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class representing a GitLab Artifact.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabArtifact {

    @JsonProperty("file_type")
    private String fileType;
    
    private String filename;
    
    private Long size;
    
    @JsonProperty("file_format")
    private String fileFormat;

    // Getters and setters
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }
}

