package com.codebridge.docker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Docker client.
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

    /**
     * Creates a Docker client configuration.
     *
     * @return the Docker client configuration
     */
    @Bean
    public DockerClientConfig dockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withRegistryUrl(registryUrl)
                .withRegistryUsername(registryUsername)
                .withRegistryPassword(registryPassword)
                .withRegistryEmail(registryEmail)
                .build();
    }

    /**
     * Creates a Docker HTTP client.
     *
     * @param config the Docker client configuration
     * @return the Docker HTTP client
     */
    @Bean
    public DockerHttpClient dockerHttpClient(DockerClientConfig config) {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
    }

    /**
     * Creates a Docker client.
     *
     * @param config the Docker client configuration
     * @param httpClient the Docker HTTP client
     * @return the Docker client
     */
    @Bean
    public DockerClient dockerClient(DockerClientConfig config, DockerHttpClient httpClient) {
        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }
}

