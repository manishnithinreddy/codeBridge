package com.codebridge.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a GitLab CI/CD pipeline.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabPipeline {
    
    private Long id;
    private String status;
    private String ref;
    private String sha;
    private String webUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long projectId;
    private String username;
    private List<GitLabJob> jobs;
}

