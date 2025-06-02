package com.codebridge.session.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "codebridge.security.jwt")
@Validated
public class IncomingJwtConfigProperties {

    @NotBlank(message = "Shared secret for validating incoming User JWTs must not be blank")
    private String sharedSecret;

    // Standard getters and setters
    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
