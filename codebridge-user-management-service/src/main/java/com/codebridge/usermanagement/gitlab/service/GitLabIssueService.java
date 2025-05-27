package com.codebridge.usermanagement.gitlab.service;

import com.codebridge.usermanagement.gitlab.model.GitLabComment;
import com.codebridge.usermanagement.gitlab.model.GitLabIssue;
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
 * Service for interacting with GitLab Issues API.
 */
@Service
public class GitLabIssueService {

    private static final Logger logger = LoggerFactory.getLogger(GitLabIssueService.class);

    private final RestTemplate restTemplate;
    
    @Value("${gitlab.api.url:https://gitlab.com/api/v4}")
    private String gitLabApiUrl;

    public GitLabIssueService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get a list of issues for a project.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param state Filter issues by state (opened, closed, all)
     * @param page Page number
     * @param perPage Number of items per page
     * @return List of issues
     */
    public List<GitLabIssue> getIssues(String projectId, String token, String state, int page, int perPage) {
        String url = String.format("%s/projects/%s/issues", gitLabApiUrl, projectId);
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("state", state)
                .queryParam("page", page)
                .queryParam("per_page", perPage);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<List<GitLabIssue>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<GitLabIssue>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching issues for project {}: {}", projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return The issue
     */
    public GitLabIssue getIssue(String projectId, int issueIid, String token) {
        String url = String.format("%s/projects/%s/issues/%d", gitLabApiUrl, projectId, issueIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabIssue> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    GitLabIssue.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching issue {} for project {}: {}", issueIid, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Create a new issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param title The issue title
     * @param description The issue description
     * @param labels Comma-separated list of labels
     * @param assigneeIds List of user IDs to assign to the issue
     * @param token The GitLab personal access token
     * @return The created issue
     */
    public GitLabIssue createIssue(String projectId, String title, String description, 
                                  String labels, List<Integer> assigneeIds, String token) {
        String url = String.format("%s/projects/%s/issues", gitLabApiUrl, projectId);
        
        HttpHeaders headers = createAuthHeaders(token);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        
        if (description != null) {
            requestBody.put("description", description);
        }
        
        if (labels != null) {
            requestBody.put("labels", labels);
        }
        
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            requestBody.put("assignee_ids", assigneeIds);
        }
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<GitLabIssue> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabIssue.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error creating issue for project {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Update an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param title The issue title
     * @param description The issue description
     * @param state The issue state (close, reopen)
     * @param labels Comma-separated list of labels
     * @param assigneeIds List of user IDs to assign to the issue
     * @param token The GitLab personal access token
     * @return The updated issue
     */
    public GitLabIssue updateIssue(String projectId, int issueIid, String title, String description,
                                  String state, String labels, List<Integer> assigneeIds, String token) {
        String url = String.format("%s/projects/%s/issues/%d", gitLabApiUrl, projectId, issueIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        
        Map<String, Object> requestBody = new HashMap<>();
        
        if (title != null) {
            requestBody.put("title", title);
        }
        
        if (description != null) {
            requestBody.put("description", description);
        }
        
        if (state != null) {
            requestBody.put("state_event", state);
        }
        
        if (labels != null) {
            requestBody.put("labels", labels);
        }
        
        if (assigneeIds != null) {
            requestBody.put("assignee_ids", assigneeIds);
        }
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<GitLabIssue> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    GitLabIssue.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error updating issue {} for project {}: {}", issueIid, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Close an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return The updated issue
     */
    public GitLabIssue closeIssue(String projectId, int issueIid, String token) {
        return updateIssue(projectId, issueIid, null, null, "close", null, null, token);
    }

    /**
     * Reopen an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return The updated issue
     */
    public GitLabIssue reopenIssue(String projectId, int issueIid, String token) {
        return updateIssue(projectId, issueIid, null, null, "reopen", null, null, token);
    }

    /**
     * Get comments (notes) for an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param token The GitLab personal access token
     * @return List of comments
     */
    public List<GitLabComment> getIssueComments(String projectId, int issueIid, String token) {
        String url = String.format("%s/projects/%s/issues/%d/notes", gitLabApiUrl, projectId, issueIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<List<GitLabComment>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<GitLabComment>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching comments for issue {} in project {}: {}", issueIid, projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Add a comment to an issue.
     *
     * @param projectId The project ID or URL-encoded path
     * @param issueIid The internal ID of the issue
     * @param body The comment text
     * @param token The GitLab personal access token
     * @return The created comment
     */
    public GitLabComment addIssueComment(String projectId, int issueIid, String body, String token) {
        String url = String.format("%s/projects/%s/issues/%d/notes", gitLabApiUrl, projectId, issueIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("body", body);
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<GitLabComment> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabComment.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error adding comment to issue {} in project {}: {}", issueIid, projectId, e.getMessage());
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

