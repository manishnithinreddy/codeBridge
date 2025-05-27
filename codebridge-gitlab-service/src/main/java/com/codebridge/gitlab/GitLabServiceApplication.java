package com.codebridge.gitlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the GitLab service.
 * This service provides integration with GitLab and general Git functionality.
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.codebridge.gitlab", "com.codebridge.gitlab.git"})
public class GitLabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitLabServiceApplication.class, args);
    }
}
