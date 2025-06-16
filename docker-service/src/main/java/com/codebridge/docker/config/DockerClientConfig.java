package com.codebridge.docker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DockerClientConfig {

    private final DockerApiConfig dockerApiConfig;

    public DockerClientConfig(DockerApiConfig dockerApiConfig) {
        this.dockerApiConfig = dockerApiConfig;
    }

    @Bean
    public DockerClient dockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerApiConfig.getHost())
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofMillis(dockerApiConfig.getConnectTimeout()))
                .responseTimeout(Duration.ofMillis(dockerApiConfig.getReadTimeout()))
                .build();

        return com.github.dockerjava.core.DockerClientImpl.getInstance(config, httpClient);
    }
}

