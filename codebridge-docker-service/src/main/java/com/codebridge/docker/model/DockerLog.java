package com.codebridge.docker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model class representing Docker container logs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerLog {

    private String id;
    
    @JsonProperty("container_id")
    private String containerId;
    
    @JsonProperty("container_name")
    private String containerName;
    
    private String stream;  // stdout or stderr
    
    private String message;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("log_level")
    private String logLevel;  // info, warn, error, debug
    
    private Map<String, String> attributes;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("line_number")
    private Long lineNumber;
    
    @JsonProperty("is_partial")
    private boolean isPartial;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isPartial() {
        return isPartial;
    }

    public void setPartial(boolean partial) {
        isPartial = partial;
    }
}

