package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.model.GitLabPipeline;

import java.util.List;

/**
 * Service for GitLab CI/CD pipeline operations.
 */
public interface GitLabPipelineService {
    
    /**
     * Gets all pipelines for a project.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return List of GitLab pipelines
     */
    List<GitLabPipeline> getPipelines(String accessToken, Long projectId);
    
    /**
     * Gets a specific pipeline by ID.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param pipelineId ID of the pipeline
     * @return GitLab pipeline or null if not found
     */
    GitLabPipeline getPipeline(String accessToken, Long projectId, Long pipelineId);
    
    /**
     * Gets all jobs for a specific pipeline.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param pipelineId ID of the pipeline
     * @return List of GitLab jobs
     */
    List<GitLabJob> getPipelineJobs(String accessToken, Long projectId, Long pipelineId);
    
    /**
     * Creates a new pipeline for a project.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param ref Branch or tag name
     * @return Created GitLab pipeline
     */
    GitLabPipeline createPipeline(String accessToken, Long projectId, String ref);
    
    /**
     * Cancels a running pipeline.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param pipelineId ID of the pipeline
     * @return Updated GitLab pipeline
     */
    GitLabPipeline cancelPipeline(String accessToken, Long projectId, Long pipelineId);
    
    /**
     * Retries a failed pipeline.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param pipelineId ID of the pipeline
     * @return Updated GitLab pipeline
     */
    GitLabPipeline retryPipeline(String accessToken, Long projectId, Long pipelineId);
}

