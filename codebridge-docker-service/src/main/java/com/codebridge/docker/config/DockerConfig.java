package com.codebridge.docker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Docker client.
 * Note: Actual Docker client configuration is now in DockerApiConfig.
 */
@Configuration
public class DockerConfig {

    @Value("${docker.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    @Value("${docker.registry.url:https://index.docker.io/v1/}")
    private String registryUrl;

    @Value("${docker.registry.username:}")
    private String registryUsername;

    @Value("${docker.registry.password:}")
    private String registryPassword;

    @Value("${docker.registry.email:}")
    private String registryEmail;

    // Configuration moved to DockerApiConfig to avoid bean definition conflicts
}

