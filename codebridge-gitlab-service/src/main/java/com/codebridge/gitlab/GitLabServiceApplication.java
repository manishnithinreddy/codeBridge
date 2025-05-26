package com.codebridge.gitlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the GitLab Integration service.
 * Handles GitLab API interactions and token management.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GitLabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitLabServiceApplication.class, args);
    }
}

