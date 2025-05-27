package com.codebridge.usermanagement.gitlab.service;

import com.codebridge.usermanagement.gitlab.model.GitLabJob;
import com.codebridge.usermanagement.gitlab.model.GitLabPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with GitLab Pipelines API.
 */
@Service
public class GitLabPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(GitLabPipelineService.class);

    private final RestTemplate restTemplate;
    
    @Value("${gitlab.api.url:https://gitlab.com/api/v4}")
    private String gitLabApiUrl;

    public GitLabPipelineService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get a list of pipelines for a project.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param ref Filter pipelines by ref (branch or tag)
     * @param status Filter pipelines by status (running, pending, success, failed, canceled, skipped)
     * @param page Page number
     * @param perPage Number of items per page
     * @return List of pipelines
     */
    public List<GitLabPipeline> getPipelines(String projectId, String token, String ref, 
                                           String status, int page, int perPage) {
        String url = String.format("%s/projects/%s/pipelines", gitLabApiUrl, projectId);
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        
        if (ref != null && !ref.isEmpty()) {
            builder.queryParam("ref", ref);
        }
        
        if (status != null && !status.isEmpty()) {
            builder.queryParam("status", status);
        }
        
        builder.queryParam("page", page)
               .queryParam("per_page", perPage);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<List<GitLabPipeline>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<GitLabPipeline>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching pipelines for project {}: {}", projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return The pipeline
     */
    public GitLabPipeline getPipeline(String projectId, int pipelineId, String token) {
        String url = String.format("%s/projects/%s/pipelines/%d", gitLabApiUrl, projectId, pipelineId);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    GitLabPipeline.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching pipeline {} for project {}: {}", pipelineId, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Get jobs for a pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return List of jobs
     */
    public List<GitLabJob> getPipelineJobs(String projectId, int pipelineId, String token) {
        String url = String.format("%s/projects/%s/pipelines/%d/jobs", gitLabApiUrl, projectId, pipelineId);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<List<GitLabJob>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<GitLabJob>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching jobs for pipeline {} in project {}: {}", 
                    pipelineId, projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get job logs.
     *
     * @param projectId The project ID or URL-encoded path
     * @param jobId The job ID
     * @param token The GitLab personal access token
     * @return The job logs
     */
    public String getJobLogs(String projectId, int jobId, String token) {
        String url = String.format("%s/projects/%s/jobs/%d/trace", gitLabApiUrl, projectId, jobId);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching logs for job {} in project {}: {}", jobId, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Create a new pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param ref The branch or tag to run the pipeline on
     * @param variables Pipeline variables
     * @param token The GitLab personal access token
     * @return The created pipeline
     */
    public GitLabPipeline createPipeline(String projectId, String ref, Map<String, String> variables, String token) {
        String url = String.format("%s/projects/%s/pipeline", gitLabApiUrl, projectId);
        
        HttpHeaders headers = createAuthHeaders(token);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ref", ref);
        
        if (variables != null && !variables.isEmpty()) {
            requestBody.put("variables", variables);
        }
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabPipeline.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error creating pipeline for project {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Retry a pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return The retried pipeline
     */
    public GitLabPipeline retryPipeline(String projectId, int pipelineId, String token) {
        String url = String.format("%s/projects/%s/pipelines/%d/retry", gitLabApiUrl, projectId, pipelineId);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabPipeline.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error retrying pipeline {} for project {}: {}", pipelineId, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Cancel a pipeline.
     *
     * @param projectId The project ID or URL-encoded path
     * @param pipelineId The pipeline ID
     * @param token The GitLab personal access token
     * @return The canceled pipeline
     */
    public GitLabPipeline cancelPipeline(String projectId, int pipelineId, String token) {
        String url = String.format("%s/projects/%s/pipelines/%d/cancel", gitLabApiUrl, projectId, pipelineId);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabPipeline.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error canceling pipeline {} for project {}: {}", pipelineId, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Create authentication headers with the GitLab token.
     *
     * @param token The GitLab personal access token
     * @return The HTTP headers
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}

