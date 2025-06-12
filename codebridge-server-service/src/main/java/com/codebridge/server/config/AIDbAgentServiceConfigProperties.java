package com.codebridge.server.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "codebridge.service-urls") // Loads all under this prefix
@Validated
public class AIDbAgentServiceConfigProperties {

    @NotBlank(message = "AI DB Agent service URL cannot be blank")
    private String aiDbAgentService; // Property name matches 'ai-db-agent-service' in yml due to kebab-case to camelCase conversion

    public String getAiDbAgentService() {
        return aiDbAgentService;
    }

    public void setAiDbAgentService(String aiDbAgentService) {
        this.aiDbAgentService = aiDbAgentService;
    }
}
