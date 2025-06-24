package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.model.GitLabPipeline;

import java.util.List;

public interface GitLabPipelineService {
    
    /**
     * Get a list of pipelines for a project
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return List of GitLab pipelines
     */
    List<GitLabPipeline> getPipelines(String accessToken, Long projectId);
    
    /**
     * Get a specific pipeline by ID
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param pipelineId ID of the pipeline
     * @return GitLab pipeline details
     */
    GitLabPipeline getPipeline(String accessToken, Long projectId, Long pipelineId);
    
    /**
     * Get jobs for a specific pipeline
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param pipelineId ID of the pipeline
     * @return List of GitLab jobs
     */
    List<GitLabJob> getPipelineJobs(String accessToken, Long projectId, Long pipelineId);
}

