package com.codebridge.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a GitLab CI/CD job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabJob {
    
    private Long id;
    private String name;
    private String stage;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String ref;
    private Long pipelineId;
    private String webUrl;
    private String runner;
    private Boolean allowFailure;
    private String user;
    private String coverage;
}

