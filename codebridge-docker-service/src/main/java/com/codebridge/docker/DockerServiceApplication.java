package com.codebridge.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Main application class for Docker service.
 */
@SpringBootApplication
public class DockerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerServiceApplication.class, args);
    }

    /**
     * Create a RestTemplate bean for HTTP requests.
     *
     * @return The RestTemplate bean
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Create an ObjectMapper bean for JSON serialization/deserialization.
     *
     * @return The ObjectMapper bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

