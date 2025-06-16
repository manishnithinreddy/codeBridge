package com.codebridge.gitlab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabJob {
    private Long id;
    private String name;
    private String stage;
    private String status;
    
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
    
    @JsonProperty("started_at")
    private ZonedDateTime startedAt;
    
    @JsonProperty("finished_at")
    private ZonedDateTime finishedAt;
    
    private Double duration;
    
    @JsonProperty("web_url")
    private String webUrl;
    
    @JsonProperty("pipeline")
    private GitLabPipeline pipeline;
    
    @JsonProperty("tag_list")
    private List<String> tagList;
}

