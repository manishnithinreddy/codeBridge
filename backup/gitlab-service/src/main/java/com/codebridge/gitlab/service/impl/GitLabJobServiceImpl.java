package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.config.GitLabApiConfig;
import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.service.GitLabJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GitLabJobServiceImpl implements GitLabJobService {

    private final GitLabApiConfig gitLabApiConfig;
    private final RestTemplate restTemplate;

    public GitLabJobServiceImpl(GitLabApiConfig gitLabApiConfig) {
        this.gitLabApiConfig = gitLabApiConfig;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<GitLabJob> getJobs(String accessToken, Long projectId) {
        log.info("Getting jobs for project: {}", projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = String.format("%s/projects/%d/jobs", gitLabApiConfig.getBaseUrl(), projectId);
            
            ResponseEntity<List<GitLabJob>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<GitLabJob>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting jobs for project: {}", projectId, e);
        }
        
        return Collections.emptyList();
    }

    @Override
    public GitLabJob getJob(String accessToken, Long projectId, Long jobId) {
        log.info("Getting job: {} for project: {}", jobId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = String.format("%s/projects/%d/jobs/%d", gitLabApiConfig.getBaseUrl(), projectId, jobId);
            
            ResponseEntity<GitLabJob> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                GitLabJob.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting job: {} for project: {}", jobId, projectId, e);
        }
        
        return null;
    }

    @Override
    public String getJobLogs(String accessToken, Long projectId, Long jobId) {
        log.info("Getting logs for job: {} in project: {}", jobId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // GitLab API endpoint for job trace (logs)
            String url = String.format("%s/projects/%d/jobs/%d/trace", gitLabApiConfig.getBaseUrl(), projectId, jobId);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting logs for job: {} in project: {}", jobId, projectId, e);
        }
        
        return "No logs available or error retrieving logs.";
    }
    
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", accessToken);
        return headers;
    }
}

