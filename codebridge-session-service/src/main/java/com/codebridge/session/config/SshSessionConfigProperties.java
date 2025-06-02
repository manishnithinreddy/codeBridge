package com.codebridge.session.config; // Adapted package

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "codebridge.session.ssh")
@Validated
public class SshSessionConfigProperties {

    /**
     * Timeout for SSH sessions in milliseconds.
     * Minimum value is 30000 (30 seconds).
     * Default value is 600000 (10 minutes).
     */
    @Min(value = 30000, message = "SSH session timeout must be at least 30000ms (30 seconds)")
    private long timeoutMs = 600000; // Default 10 minutes

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
