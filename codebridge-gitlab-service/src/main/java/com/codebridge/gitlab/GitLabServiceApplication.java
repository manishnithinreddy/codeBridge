package com.codebridge.gitlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for the GitLab service.
 * This service provides integration with GitLab and general Git functionality.
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.codebridge.gitlab"})
@EnableJpaRepositories(basePackages = {"com.codebridge.gitlab.git.repository"})
public class GitLabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitLabServiceApplication.class, args);
    }
}
