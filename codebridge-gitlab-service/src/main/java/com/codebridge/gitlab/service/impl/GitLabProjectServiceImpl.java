package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.model.GitLabProject;
import com.codebridge.gitlab.service.GitLabProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of GitLabProjectService.
 */
@Slf4j
@Service
public class GitLabProjectServiceImpl implements GitLabProjectService {

    private final RestTemplate restTemplate;
    private final String gitLabApiBaseUrl;

    public GitLabProjectServiceImpl(
            RestTemplate restTemplate,
            @Value("${gitlab.api.base-url}") String gitLabApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.gitLabApiBaseUrl = gitLabApiBaseUrl;
    }

    @Override
    public List<GitLabProject> getProjects(String accessToken) {
        log.info("Getting all accessible projects");
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<List<GitLabProject>> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects?membership=true&per_page=100",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<GitLabProject>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting projects", e);
            return Collections.emptyList();
        }
    }

    @Override
    public GitLabProject getProject(String accessToken, Long projectId) {
        log.info("Getting project: {}", projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabProject> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId,
                    HttpMethod.GET,
                    requestEntity,
                    GitLabProject.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting project: {}", projectId, e);
            return null;
        }
    }

    @Override
    public GitLabProject getProjectByPath(String accessToken, String pathWithNamespace) {
        log.info("Getting project by path: {}", pathWithNamespace);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            String encodedPath = URLEncoder.encode(pathWithNamespace, StandardCharsets.UTF_8);
            String url = UriComponentsBuilder.fromHttpUrl(gitLabApiBaseUrl + "/projects/" + encodedPath)
                    .build()
                    .toUriString();
            
            ResponseEntity<GitLabProject> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    GitLabProject.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting project by path: {}", pathWithNamespace, e);
            return null;
        }
    }

    @Override
    public GitLabProject createProject(String accessToken, String name, String description, String visibility) {
        log.info("Creating project: {}", name);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", name);
            requestBody.put("description", description);
            requestBody.put("visibility", visibility);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<GitLabProject> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects",
                    HttpMethod.POST,
                    requestEntity,
                    GitLabProject.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating project: {}", name, e);
            return null;
        }
    }

    @Override
    public GitLabProject archiveProject(String accessToken, Long projectId) {
        log.info("Archiving project: {}", projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabProject> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/archive",
                    HttpMethod.POST,
                    requestEntity,
                    GitLabProject.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error archiving project: {}", projectId, e);
            return null;
        }
    }

    @Override
    public GitLabProject unarchiveProject(String accessToken, Long projectId) {
        log.info("Unarchiving project: {}", projectId);
        
        try {
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<GitLabProject> response = restTemplate.exchange(
                    gitLabApiBaseUrl + "/projects/" + projectId + "/unarchive",
                    HttpMethod.POST,
                    requestEntity,
                    GitLabProject.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error unarchiving project: {}", projectId, e);
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

