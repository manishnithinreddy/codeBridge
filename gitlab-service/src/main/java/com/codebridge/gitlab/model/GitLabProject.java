package com.codebridge.gitlab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabProject {
    private Long id;
    private String name;
    private String description;
    
    @JsonProperty("name_with_namespace")
    private String nameWithNamespace;
    
    @JsonProperty("path_with_namespace")
    private String pathWithNamespace;
    
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
    
    @JsonProperty("last_activity_at")
    private ZonedDateTime lastActivityAt;
    
    @JsonProperty("web_url")
    private String webUrl;
    
    @JsonProperty("readme_url")
    private String readmeUrl;
    
    @JsonProperty("default_branch")
    private String defaultBranch;
    
    @JsonProperty("visibility")
    private String visibility;
}

