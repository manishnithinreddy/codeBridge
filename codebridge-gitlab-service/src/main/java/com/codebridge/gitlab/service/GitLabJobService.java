package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.GitLabJob;

import java.util.List;

/**
 * Service for GitLab CI/CD job operations.
 */
public interface GitLabJobService {
    
    /**
     * Gets all jobs for a project.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return List of GitLab jobs
     */
    List<GitLabJob> getJobs(String accessToken, Long projectId);
    
    /**
     * Gets a specific job by ID.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param jobId ID of the job
     * @return GitLab job or null if not found
     */
    GitLabJob getJob(String accessToken, Long projectId, Long jobId);
    
    /**
     * Gets logs for a specific job.
     *
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param jobId ID of the job
     * @return Job logs as a string
     */
    String getJobLogs(String accessToken, Long projectId, Long jobId);
}

