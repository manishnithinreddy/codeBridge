package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.GitLabProject;

import java.util.List;

public interface GitLabProjectService {
    
    /**
     * Get a list of projects accessible by the authenticated user
     * 
     * @param accessToken GitLab personal access token
     * @return List of GitLab projects
     */
    List<GitLabProject> getProjects(String accessToken);
    
    /**
     * Get a specific project by ID
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project to retrieve
     * @return GitLab project details
     */
    GitLabProject getProject(String accessToken, Long projectId);
}

