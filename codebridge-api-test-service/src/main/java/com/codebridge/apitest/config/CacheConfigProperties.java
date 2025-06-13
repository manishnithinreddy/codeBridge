package com.codebridge.apitest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for caching.
 */
@Configuration
@ConfigurationProperties(prefix = "codebridge.cache")
public class CacheConfigProperties {
    
    private List<String> cacheableHeaders;
    private long defaultTtlSeconds;
    
    public List<String> getCacheableHeaders() {
        return cacheableHeaders;
    }
    
    public void setCacheableHeaders(List<String> cacheableHeaders) {
        this.cacheableHeaders = cacheableHeaders;
    }
    
    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }
    
    public void setDefaultTtlSeconds(long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }
}

