package com.codebridge.usermanagement.gitlab.service;

import com.codebridge.usermanagement.gitlab.model.*;
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
 * Service for interacting with GitLab Merge Request API.
 */
@Service
public class GitLabMergeRequestService {

    private static final Logger logger = LoggerFactory.getLogger(GitLabMergeRequestService.class);

    private final RestTemplate restTemplate;
    
    @Value("${gitlab.api.url:https://gitlab.com/api/v4}")
    private String gitLabApiUrl;

    public GitLabMergeRequestService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get a list of merge requests for a project.
     *
     * @param projectId The project ID or URL-encoded path
     * @param token The GitLab personal access token
     * @param state Filter merge requests by state (opened, closed, locked, merged)
     * @param page Page number
     * @param perPage Number of items per page
     * @return List of merge requests
     */
    public List<GitLabMergeRequest> getMergeRequests(String projectId, String token, String state, int page, int perPage) {
        String url = String.format("%s/projects/%s/merge_requests", gitLabApiUrl, projectId);
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("state", state)
                .queryParam("page", page)
                .queryParam("per_page", perPage);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<List<GitLabMergeRequest>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<GitLabMergeRequest>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching merge requests for project {}: {}", projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return The merge request
     */
    public GitLabMergeRequest getMergeRequest(String projectId, int mergeRequestIid, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d", gitLabApiUrl, projectId, mergeRequestIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabMergeRequest> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    GitLabMergeRequest.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching merge request {} for project {}: {}", mergeRequestIid, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Get the changes (diff) for a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return The merge request diff
     */
    public GitLabMergeRequestDiff getMergeRequestDiff(String projectId, int mergeRequestIid, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/changes", gitLabApiUrl, projectId, mergeRequestIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabMergeRequestDiff> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    GitLabMergeRequestDiff.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching diff for merge request {} in project {}: {}", mergeRequestIid, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Get comments (notes) for a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return List of comments
     */
    public List<GitLabComment> getMergeRequestComments(String projectId, int mergeRequestIid, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/notes", gitLabApiUrl, projectId, mergeRequestIid);
        
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
            logger.error("Error fetching comments for merge request {} in project {}: {}", mergeRequestIid, projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Add a comment to a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param body The comment text
     * @param token The GitLab personal access token
     * @return The created comment
     */
    public GitLabComment addMergeRequestComment(String projectId, int mergeRequestIid, String body, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/notes", gitLabApiUrl, projectId, mergeRequestIid);
        
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
            logger.error("Error adding comment to merge request {} in project {}: {}", mergeRequestIid, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Add an inline comment to a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param body The comment text
     * @param path The file path
     * @param line The line number
     * @param lineType The line type (new or old)
     * @param token The GitLab personal access token
     * @return The created comment
     */
    public GitLabComment addInlineComment(String projectId, int mergeRequestIid, String body, 
                                         String path, int line, String lineType, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/notes", gitLabApiUrl, projectId, mergeRequestIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", body);
        requestBody.put("path", path);
        requestBody.put("line", line);
        requestBody.put("line_type", lineType);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<GitLabComment> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabComment.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error adding inline comment to merge request {} in project {}: {}", 
                    mergeRequestIid, projectId, e.getMessage());
            return null;
        }
    }

    /**
     * Get approval information for a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return The approval information
     */
    public List<GitLabApprovalRule> getMergeRequestApprovalRules(String projectId, int mergeRequestIid, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/approval_rules", 
                gitLabApiUrl, projectId, mergeRequestIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<List<GitLabApprovalRule>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<GitLabApprovalRule>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching approval rules for merge request {} in project {}: {}", 
                    mergeRequestIid, projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Approve a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return True if approved successfully
     */
    public boolean approveMergeRequest(String projectId, int mergeRequestIid, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/approve", 
                gitLabApiUrl, projectId, mergeRequestIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<GitLabApproval> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GitLabApproval.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error approving merge request {} in project {}: {}", 
                    mergeRequestIid, projectId, e.getMessage());
            return false;
        }
    }

    /**
     * Unapprove a merge request.
     *
     * @param projectId The project ID or URL-encoded path
     * @param mergeRequestIid The internal ID of the merge request
     * @param token The GitLab personal access token
     * @return True if unapproved successfully
     */
    public boolean unapproveMergeRequest(String projectId, int mergeRequestIid, String token) {
        String url = String.format("%s/projects/%s/merge_requests/%d/unapprove", 
                gitLabApiUrl, projectId, mergeRequestIid);
        
        HttpHeaders headers = createAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Void.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error unapproving merge request {} in project {}: {}", 
                    mergeRequestIid, projectId, e.getMessage());
            return false;
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

