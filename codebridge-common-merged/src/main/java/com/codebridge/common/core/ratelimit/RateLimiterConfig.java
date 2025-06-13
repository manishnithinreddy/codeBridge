package com.codebridge.core.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for distributed rate limiting using Bucket4j and JCache.
 */
@Configuration
public class RateLimiterConfig {

    @Value("${codebridge.ratelimit.capacity:100}")
    private int capacity;

    @Value("${codebridge.ratelimit.refill-tokens:10}")
    private int refillTokens;

    @Value("${codebridge.ratelimit.refill-duration:1}")
    private int refillDuration;

    @Value("${codebridge.ratelimit.cache-expiry:3600}")
    private int cacheExpiry;

    /**
     * Creates a JCache cache manager for rate limiting.
     *
     * @return The cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        return Caching.getCachingProvider().getCacheManager();
    }

    /**
     * Creates a cache for storing rate limit buckets.
     *
     * @param cacheManager The cache manager
     * @return The cache
     */
    @Bean
    public Cache<String, byte[]> rateLimitCache(CacheManager cacheManager) {
        MutableConfiguration<String, byte[]> config = new MutableConfiguration<String, byte[]>()
                .setTypes(String.class, byte[].class)
                .setStoreByValue(false)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, cacheExpiry)));

        return cacheManager.createCache("rate-limit-cache", config);
    }

    /**
     * Creates a proxy manager for distributed rate limiting.
     *
     * @param cache The cache for storing rate limit buckets
     * @return The proxy manager
     */
    @Bean
    public ProxyManager<String> proxyManager(Cache<String, byte[]> rateLimitCache) {
        return new JCacheProxyManager<>(rateLimitCache);
    }

    /**
     * Creates a bandwidth configuration for rate limiting.
     *
     * @return The bandwidth configuration
     */
    @Bean
    public Bandwidth bandwidth() {
        return Bandwidth.classic(capacity, Refill.intervally(refillTokens, java.time.Duration.of(refillDuration, ChronoUnit.MINUTES)));
    }

    /**
     * Creates a bucket for a specific key.
     *
     * @param proxyManager The proxy manager
     * @param bandwidth The bandwidth configuration
     * @param key The key to create a bucket for
     * @return The bucket
     */
    public Bucket resolveBucket(ProxyManager<String> proxyManager, Bandwidth bandwidth, String key) {
        return proxyManager.builder().build(key, () -> bandwidth);
    }
}

