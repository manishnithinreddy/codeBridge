package com.codebridge.session.config; // Adapted package

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtConfigProperties {

    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    @Min(value = 60000, message = "JWT expiration time must be at least 60000ms (1 minute)")
    private long expirationMs = 600000; // Default 10 minutes

    // Getters and setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
