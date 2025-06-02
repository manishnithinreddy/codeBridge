package com.codebridge.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "codebridge.session.db")
@Validated
public class DbSessionConfigProperties {

    /**
     * Timeout for Database sessions in milliseconds.
     * Minimum value is 30000 (30 seconds).
     * Default value is 300000 (5 minutes).
     */
    @Min(value = 30000, message = "Database session timeout must be at least 30000ms (30 seconds)")
    private long timeoutMs = 300000; // Default 5 minutes

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
