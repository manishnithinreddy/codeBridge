package com.codebridge.session.config;

import javax.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "codebridge.session.db")
@Validated
public class DbSessionConfigProperties {

    @Positive(message = "Default DB session timeout (ms) must be positive")
    private long defaultTimeoutMs = 1800000; // Default to 30 minutes

    @Positive(message = "Max active DB sessions per user per alias must be positive")
    private int maxSessionsPerUserPerAlias = 3; // Default

    // Getters and Setters
    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public int getMaxSessionsPerUserPerAlias() {
        return maxSessionsPerUserPerAlias;
    }

    public void setMaxSessionsPerUserPerAlias(int maxSessionsPerUserPerAlias) {
        this.maxSessionsPerUserPerAlias = maxSessionsPerUserPerAlias;
    }
}
