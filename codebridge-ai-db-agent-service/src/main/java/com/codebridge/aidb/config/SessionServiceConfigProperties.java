package com.codebridge.aidb.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "codebridge.service-urls")
@Validated
public class SessionServiceConfigProperties {

    @NotBlank(message = "Session Service API base URL cannot be blank")
    private String sessionServiceApiBaseUrl;

    public String getSessionServiceApiBaseUrl() {
        return sessionServiceApiBaseUrl;
    }

    public void setSessionServiceApiBaseUrl(String sessionServiceApiBaseUrl) {
        this.sessionServiceApiBaseUrl = sessionServiceApiBaseUrl;
    }
}
