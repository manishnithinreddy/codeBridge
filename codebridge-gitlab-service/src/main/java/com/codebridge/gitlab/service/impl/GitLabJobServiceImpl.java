package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.service.GitLabJobService;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Collections;
import java.util.List;

/**
 * Implementation of GitLabJobService.
 */
@Slf4j
@Service
public class GitLabJobServiceImpl implements GitLabJobService {

    private static final Logger log = LoggerFactory.getLogger(GitLabJobServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final String gitLabApiBaseUrl;

    public GitLabJobServiceImpl(
            RestTemplate restTemplate,
            @Value("${gitlab.api.base-url}") String gitLabApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.gitLabApiBaseUrl = gitLabApiBaseUrl;
    }

    @Override
    public List<GitLabJob> getJobs(String accessToken, Long projectId) {
        log.info("Getting jobs for project: {}", projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<List<GitLabJob>> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/jobs",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<GitLabJob>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting jobs for project: {}", projectId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public GitLabJob getJob(String accessToken, Long projectId, Long jobId) {
        log.info("Getting job: {} for project: {}", jobId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabJob> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/jobs/" + jobId,
                    HttpMethod.GET,
                    requestEntity,
                    GitLabJob.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting job: {} for project: {}", jobId, projectId, e);
            return null;
        }
    }

    @Override
    public String getJobLogs(String accessToken, Long projectId, Long jobId) {
        log.info("Getting logs for job: {} in project: {}", jobId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/jobs/" + jobId + "/trace",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting logs for job: {} in project: {}", jobId, projectId, e);
            return "Error retrieving job logs: " + e.getMessage();
        }
    }
    
    /**
     * Creates HTTP headers with GitLab personal access token.
     *
     * @param accessToken GitLab personal access token
     * @return HTTP headers
     */
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", accessToken);
        return headers;
    }
}
