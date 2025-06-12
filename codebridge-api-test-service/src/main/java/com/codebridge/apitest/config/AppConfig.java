package com.codebridge.apitest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration.
 */
@Configuration
public class AppConfig {

    /**
     * Creates a RestTemplate bean.
     *
     * @return the RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

