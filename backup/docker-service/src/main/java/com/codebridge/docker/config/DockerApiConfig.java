package com.codebridge.docker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "docker.api")
public class DockerApiConfig {
    private String host;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
}

