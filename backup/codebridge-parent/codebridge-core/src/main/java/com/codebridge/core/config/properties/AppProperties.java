package com.codebridge.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private String name = "CodeBridge";
    private String version = "0.1.0";
    private String description = "Cross-platform developer tool that centralizes workflows";
    private Security security = new Security();
    private Cors cors = new Cors();
    
    @Getter
    @Setter
    public static class Security {
        private String tokenSecret;
        private long tokenExpirationMs = 86400000; // 1 day
        private long refreshTokenExpirationMs = 604800000; // 7 days
    }
    
    @Getter
    @Setter
    public static class Cors {
        private String[] allowedOrigins = {"*"};
        private String[] allowedMethods = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};
        private String[] allowedHeaders = {"*"};
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }
}

