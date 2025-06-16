package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.GitLabProject;

import java.util.List;

/**
 * Service for GitLab project operations.
 */
public interface GitLabProjectService {
    
    /**
     * Gets all accessible projects.
     *
     * @param accessToken GitLab personal access token
     * @return List of GitLab projects
     */
    List<GitLabProject> getProjects(String accessToken);
    
    /**
     * Gets a specific project by ID.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return GitLab project or null if not found
     */
    GitLabProject getProject(String accessToken, Long projectId);
    
    /**
     * Gets a specific project by path with namespace.
     *
     * @param accessToken GitLab personal access token
     * @param pathWithNamespace Path with namespace (e.g., "group/project")
     * @return GitLab project or null if not found
     */
    GitLabProject getProjectByPath(String accessToken, String pathWithNamespace);
    
    /**
     * Creates a new project.
     *
     * @param accessToken GitLab personal access token
     * @param name Name of the project
     * @param description Description of the project
     * @param visibility Visibility level (private, internal, public)
     * @return Created GitLab project
     */
    GitLabProject createProject(String accessToken, String name, String description, String visibility);
    
    /**
     * Archives a project.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return Updated GitLab project
     */
    GitLabProject archiveProject(String accessToken, Long projectId);
    
    /**
     * Unarchives a project.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return Updated GitLab project
     */
    GitLabProject unarchiveProject(String accessToken, Long projectId);
}

