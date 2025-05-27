package com.codebridge.usermanagement.gitlab.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for GitLab integration.
 */
@Configuration
public class GitLabConfig {

    /**
     * Creates a RestTemplate bean for GitLab API calls.
     *
     * @param builder The RestTemplateBuilder
     * @return The configured RestTemplate
     */
    @Bean("gitLabRestTemplate")
    public RestTemplate gitLabRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}

