package com.codebridge.gitlab.service;

import com.codebridge.gitlab.model.GitLabJob;

import java.util.List;

public interface GitLabJobService {
    
    /**
     * Get a list of jobs for a project
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @return List of GitLab jobs
     */
    List<GitLabJob> getJobs(String accessToken, Long projectId);
    
    /**
     * Get a specific job by ID
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param jobId ID of the job
     * @return GitLab job details
     */
    GitLabJob getJob(String accessToken, Long projectId, Long jobId);
    
    /**
     * Get the log output of a specific job
     * 
     * @param accessToken GitLab personal access token
     * @param projectId ID of the project
     * @param jobId ID of the job
     * @return Job log output as a string
     */
    String getJobLogs(String accessToken, Long projectId, Long jobId);
}

