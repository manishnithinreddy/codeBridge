package com.codebridge.gitlab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabPipeline {
    private Long id;
    
    @JsonProperty("project_id")
    private Long projectId;
    
    private String status;
    private String ref;
    private String sha;
    
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
    
    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;
    
    @JsonProperty("web_url")
    private String webUrl;
}

