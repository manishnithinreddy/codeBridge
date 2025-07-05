package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.model.GitLabPipeline;
import com.codebridge.gitlab.service.GitLabPipelineService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of GitLabPipelineService.
 */
@Slf4j
@Service
public class GitLabPipelineServiceImpl implements GitLabPipelineService {

    private static final Logger log = LoggerFactory.getLogger(GitLabPipelineServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final String gitLabApiBaseUrl;

    public GitLabPipelineServiceImpl(
            RestTemplate restTemplate,
            @Value("${gitlab.api.base-url}") String gitLabApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.gitLabApiBaseUrl = gitLabApiBaseUrl;
    }

    @Override
    public List<GitLabPipeline> getPipelines(String accessToken, Long projectId) {
        log.info("Getting pipelines for project: {}", projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<List<GitLabPipeline>> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/pipelines",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<GitLabPipeline>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting pipelines for project: {}", projectId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public GitLabPipeline getPipeline(String accessToken, Long projectId, Long pipelineId) {
        log.info("Getting pipeline: {} for project: {}", pipelineId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/pipelines/" + pipelineId,
                    HttpMethod.GET,
                    requestEntity,
                    GitLabPipeline.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting pipeline: {} for project: {}", pipelineId, projectId, e);
            return null;
        }
    }

    @Override
    public List<GitLabJob> getPipelineJobs(String accessToken, Long projectId, Long pipelineId) {
        log.info("Getting jobs for pipeline: {} in project: {}", pipelineId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<List<GitLabJob>> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/pipelines/" + pipelineId + "/jobs",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<GitLabJob>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting jobs for pipeline: {} in project: {}", pipelineId, projectId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public GitLabPipeline createPipeline(String accessToken, Long projectId, String ref) {
        log.info("Creating pipeline for project: {} with ref: {}", projectId, ref);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("ref", ref);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/pipeline",
                    HttpMethod.POST,
                    requestEntity,
                    GitLabPipeline.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating pipeline for project: {} with ref: {}", projectId, ref, e);
            return null;
        }
    }

    @Override
    public GitLabPipeline cancelPipeline(String accessToken, Long projectId, Long pipelineId) {
        log.info("Cancelling pipeline: {} for project: {}", pipelineId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/pipelines/" + pipelineId + "/cancel",
                    HttpMethod.POST,
                    requestEntity,
                    GitLabPipeline.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error cancelling pipeline: {} for project: {}", pipelineId, projectId, e);
            return null;
        }
    }

    @Override
    public GitLabPipeline retryPipeline(String accessToken, Long projectId, Long pipelineId) {
        log.info("Retrying pipeline: {} for project: {}", pipelineId, projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabPipeline> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/pipelines/" + pipelineId + "/retry",
                    HttpMethod.POST,
                    requestEntity,
                    GitLabPipeline.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrying pipeline: {} for project: {}", pipelineId, projectId, e);
            return null;
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
