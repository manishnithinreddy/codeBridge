package com.codebridge.session.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "codebridge.session.ssh")
@Validated
public class SshSessionConfigProperties {

    @Positive(message = "Default session timeout (ms) must be positive")
    private long defaultTimeoutMs = 300000; // Default to 5 minutes

    @Positive(message = "Max active sessions per user per server must be positive")
    private int maxSessionsPerUserPerServer = 5; // Default

    // Getters and Setters
    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public int getMaxSessionsPerUserPerServer() {
        return maxSessionsPerUserPerServer;
    }

    public void setMaxSessionsPerUserPerServer(int maxSessionsPerUserPerServer) {
        this.maxSessionsPerUserPerServer = maxSessionsPerUserPerServer;
    }
}
