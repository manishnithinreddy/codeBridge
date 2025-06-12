package com.codebridge.apitest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for response caching.
 */
@Configuration
@ConfigurationProperties(prefix = "codebridge.cache")
public class CacheConfigProperties {
    
    /**
     * Default time-to-live for cached responses in seconds.
     */
    private long defaultTtlSeconds = 300; // 5 minutes
    
    /**
     * Maximum size of the local cache.
     */
    private int maxLocalCacheSize = 1000;
    
    /**
     * List of headers that should be included in the cache key.
     */
    private List<String> cacheableHeaders = Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "X-API-Key"
    );
    
    /**
     * Whether caching is enabled.
     */
    private boolean enabled = true;

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public int getMaxLocalCacheSize() {
        return maxLocalCacheSize;
    }

    public void setMaxLocalCacheSize(int maxLocalCacheSize) {
        this.maxLocalCacheSize = maxLocalCacheSize;
    }

    public List<String> getCacheableHeaders() {
        return cacheableHeaders;
    }

    public void setCacheableHeaders(List<String> cacheableHeaders) {
        this.cacheableHeaders = cacheableHeaders;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

