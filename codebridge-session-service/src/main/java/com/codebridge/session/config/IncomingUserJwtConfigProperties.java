package com.codebridge.session.config;

import javax.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

// This class specifically loads the shared secret for validating incoming User JWTs
@Configuration
@ConfigurationProperties(prefix = "codebridge.security.jwt") // Matches the key in application.yml
@Validated
public class IncomingUserJwtConfigProperties {

    @NotBlank(message = "Shared secret for validating incoming User JWTs cannot be blank")
    private String sharedSecret;

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
