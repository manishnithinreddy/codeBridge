package com.codebridge.gitlab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for GitLab API client.
 */
@Configuration
public class GitLabApiConfig {

    @Value("${gitlab.api.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${gitlab.api.read-timeout:30000}")
    private long readTimeout;

    @Value("${gitlab.api.write-timeout:10000}")
    private long writeTimeout;

    /**
     * Creates a RestTemplate bean configured for GitLab API.
     *
     * @param builder RestTemplateBuilder
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}

