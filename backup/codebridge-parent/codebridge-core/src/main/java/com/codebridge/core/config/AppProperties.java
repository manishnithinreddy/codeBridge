package com.codebridge.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private final Security security = new Security();
    private final Cors cors = new Cors();
    private final Token token = new Token();

    @Data
    public static class Security {
        private String issuerUri;
        private String resourceId;
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private long maxAgeInSeconds = 3600;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private boolean allowCredentials = true;
        private long maxAgeInSeconds = 3600;
    }

    @Data
    public static class Token {
        private long accessTokenExpirationMs = 900000; // 15 minutes
        private long refreshTokenExpirationMs = 2592000000L; // 30 days
        private long apiTokenExpirationMs = 31536000000L; // 1 year
        private String secret;
        private String issuer = "codebridge";
    }
}

