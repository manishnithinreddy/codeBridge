package com.codebridge.docker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Docker API client.
 */
@Configuration
public class DockerApiConfig {

    @Value("${docker.api.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    @Value("${docker.api.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${docker.api.read-timeout:30000}")
    private long readTimeout;

    @Value("${docker.api.write-timeout:10000}")
    private long writeTimeout;

    /**
     * Creates a DockerClientConfig bean.
     *
     * @return Configured DockerClientConfig
     */
    @Bean
    public DockerClientConfig dockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
    }

    /**
     * Creates a DockerHttpClient bean.
     *
     * @param dockerClientConfig DockerClientConfig
     * @return Configured DockerHttpClient
     */
    @Bean
    public DockerHttpClient dockerHttpClient(DockerClientConfig dockerClientConfig) {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .connectionTimeout(Duration.ofMillis(connectTimeout))
                .responseTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    /**
     * Creates a DockerClient bean.
     *
     * @param dockerClientConfig DockerClientConfig
     * @param dockerHttpClient DockerHttpClient
     * @return Configured DockerClient
     */
    @Bean
    public DockerClient dockerClient(DockerClientConfig dockerClientConfig, DockerHttpClient dockerHttpClient) {
        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }
}

