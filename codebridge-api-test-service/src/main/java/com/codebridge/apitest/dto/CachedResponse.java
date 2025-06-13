package com.codebridge.apitest.dto;

import java.util.Map;

/**
 * DTO for cached API responses.
 */
public class CachedResponse {
    private int statusCode;
    private String responseBody;
    private Map<String, String> responseHeaders;
    private long cachedAt;

    public CachedResponse() {
        // Default constructor for Jackson
    }

    public CachedResponse(int statusCode, String responseBody, Map<String, String> responseHeaders, long cachedAt) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
        this.cachedAt = cachedAt;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public long getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(long cachedAt) {
        this.cachedAt = cachedAt;
    }
    
    /**
     * Checks if the cached response is still fresh based on a TTL.
     * 
     * @param ttlSeconds The time-to-live in seconds
     * @return true if the cached response is still fresh, false otherwise
     */
    public boolean isFresh(long ttlSeconds) {
        long now = System.currentTimeMillis();
        long expiresAt = cachedAt + (ttlSeconds * 1000);
        return now < expiresAt;
    }
}

