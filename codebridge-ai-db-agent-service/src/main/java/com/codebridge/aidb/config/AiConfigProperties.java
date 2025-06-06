package com.codebridge.aidb.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "codebridge.ai.text-to-sql")
@Validated
public class AiConfigProperties {

    @NotBlank(message = "AI Text-to-SQL endpoint URL cannot be blank")
    private String endpointUrl;

    @NotBlank(message = "AI Text-to-SQL API key cannot be blank")
    private String apiKey;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
