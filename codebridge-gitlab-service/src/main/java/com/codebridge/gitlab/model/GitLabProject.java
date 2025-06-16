package com.codebridge.gitlab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a GitLab project.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabProject {
    
    private Long id;
    private String name;
    private String description;
    private String defaultBranch;
    private String webUrl;
    private String sshUrlToRepo;
    private String httpUrlToRepo;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private String visibility;
    private String namespace;
    private Boolean archived;
    private Boolean forked;
    private Integer forksCount;
    private Integer starCount;
}

