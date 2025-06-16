package com.codebridge.gitlab.controller;

import com.codebridge.gitlab.model.GitLabJob;
import com.codebridge.gitlab.service.GitLabJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/projects/{projectId}/jobs")
public class GitLabJobController {

    private final GitLabJobService gitLabJobService;

    public GitLabJobController(GitLabJobService gitLabJobService) {
        this.gitLabJobService = gitLabJobService;
    }

    @GetMapping
    public ResponseEntity<List<GitLabJob>> getJobs(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long projectId) {
        log.info("Getting jobs for project: {}", projectId);
        
        String accessToken = extractToken(authHeader);
        List<GitLabJob> jobs = gitLabJobService.getJobs(accessToken, projectId);
        
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<GitLabJob> getJob(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long projectId,
            @PathVariable Long jobId) {
        log.info("Getting job: {} for project: {}", jobId, projectId);
        
        String accessToken = extractToken(authHeader);
        GitLabJob job = gitLabJobService.getJob(accessToken, projectId, jobId);
        
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(job);
    }

    @GetMapping("/{jobId}/logs")
    public ResponseEntity<String> getJobLogs(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long projectId,
            @PathVariable Long jobId) {
        log.info("Getting logs for job: {} in project: {}", jobId, projectId);
        
        String accessToken = extractToken(authHeader);
        String logs = gitLabJobService.getJobLogs(accessToken, projectId, jobId);
        
        return ResponseEntity.ok(logs);
    }
    
    private String extractToken(String authHeader) {
        // For Bearer token, extract the token part
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}

