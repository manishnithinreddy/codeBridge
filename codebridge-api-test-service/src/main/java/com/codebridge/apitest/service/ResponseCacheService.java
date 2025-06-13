package com.codebridge.apitest.service;

import com.codebridge.apitest.config.CacheConfigProperties;
import com.codebridge.apitest.dto.CachedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching API responses to improve performance.
 */
@Service
public class ResponseCacheService {
    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheConfigProperties cacheConfig;
    
    // Local in-memory cache for faster access to frequently used responses
    private final ConcurrentHashMap<String, CachedResponse> localCache = new ConcurrentHashMap<>();
    
    @Autowired
    public ResponseCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            CacheConfigProperties cacheConfig) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheConfig = cacheConfig;
    }
    
    /**
     * Generates a cache key based on the request parameters.
     * 
     * @param url The request URL
     * @param method The HTTP method
     * @param headers The request headers
     * @param body The request body
     * @return A unique cache key
     */
    public String generateCacheKey(String url, String method, Map<String, String> headers, String body) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method).append(":").append(url);
        
        // Add cacheable headers to the key
        if (headers != null && !headers.isEmpty()) {
            for (String cacheableHeader : cacheConfig.getCacheableHeaders()) {
                if (headers.containsKey(cacheableHeader)) {
                    keyBuilder.append(":").append(cacheableHeader).append("=").append(headers.get(cacheableHeader));
                }
            }
        }
        
        // Add body hash if present and method is not GET
        if (body != null && !body.isEmpty() && !"GET".equalsIgnoreCase(method)) {
            keyBuilder.append(":body=").append(body.hashCode());
        }
        
        return "api-response:" + keyBuilder.toString().hashCode();
    }
    
    /**
     * Gets a cached response if available.
     * 
     * @param cacheKey The cache key
     * @return An Optional containing the cached response if found
     */
    public Optional<CachedResponse> getCachedResponse(String cacheKey) {
        // First check local cache for faster access
        CachedResponse localCachedResponse = localCache.get(cacheKey);
        if (localCachedResponse != null) {
            logger.debug("Cache hit (local) for key: {}", cacheKey);
            return Optional.of(localCachedResponse);
        }
        
        // Then check Redis
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                CachedResponse cachedResponse = objectMapper.readValue(cachedJson, CachedResponse.class);
                // Store in local cache for faster future access
                localCache.put(cacheKey, cachedResponse);
                logger.debug("Cache hit (Redis) for key: {}", cacheKey);
                return Optional.of(cachedResponse);
            } catch (JsonProcessingException e) {
                logger.error("Error deserializing cached response: {}", e.getMessage(), e);
            }
        }
        
        logger.debug("Cache miss for key: {}", cacheKey);
        return Optional.empty();
    }
    
    /**
     * Caches an API response.
     * 
     * @param cacheKey The cache key
     * @param statusCode The response status code
     * @param responseBody The response body
     * @param responseHeaders The response headers
     * @param ttlSeconds Time-to-live in seconds
     */
    public void cacheResponse(String cacheKey, int statusCode, String responseBody, 
                             Map<String, String> responseHeaders, long ttlSeconds) {
        // Only cache successful responses
        if (statusCode >= 200 && statusCode < 300) {
            CachedResponse cachedResponse = new CachedResponse(
                    statusCode, responseBody, responseHeaders, System.currentTimeMillis());
            
            try {
                String cachedJson = objectMapper.writeValueAsString(cachedResponse);
                
                // Store in Redis with TTL
                redisTemplate.opsForValue().set(cacheKey, cachedJson, ttlSeconds, TimeUnit.SECONDS);
                
                // Store in local cache
                localCache.put(cacheKey, cachedResponse);
                
                logger.debug("Cached response for key: {} with TTL: {} seconds", cacheKey, ttlSeconds);
            } catch (JsonProcessingException e) {
                logger.error("Error serializing response for caching: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("Not caching non-successful response with status code: {}", statusCode);
        }
    }
    
    /**
     * Invalidates a cached response.
     * 
     * @param cacheKey The cache key
     */
    public void invalidateCache(String cacheKey) {
        localCache.remove(cacheKey);
        redisTemplate.delete(cacheKey);
        logger.debug("Invalidated cache for key: {}", cacheKey);
    }
    
    /**
     * Clears all cached responses.
     */
    public void clearAllCaches() {
        localCache.clear();
        // In a real implementation, you might want to be more selective about which Redis keys to delete
        logger.info("Cleared all response caches");
    }
    
    /**
     * Gets cache statistics.
     * 
     * @return Map of cache statistics
     */
    public Map<String, Object> getCacheStats() {
        long localCacheSize = localCache.size();
        
        return Map.of(
                "localCacheSize", localCacheSize,
                "cacheableHeaders", cacheConfig.getCacheableHeaders(),
                "defaultTtlSeconds", cacheConfig.getDefaultTtlSeconds()
        );
    }
}

