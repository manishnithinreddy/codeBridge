package com.codebridge.server.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "codebridge.security.jwt")
@Validated // Enable validation on these properties
public class IncomingUserJwtConfigProperties {

    @NotBlank(message = "Shared secret for User JWT validation cannot be blank")
    private String sharedSecret;

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
