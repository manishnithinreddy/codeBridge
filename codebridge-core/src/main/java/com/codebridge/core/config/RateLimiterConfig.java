package com.codebridge.core.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for rate limiting.
 * Uses Bucket4j for token bucket algorithm implementation.
 */
@Configuration
public class RateLimiterConfig {

    @Value("${rate-limiter.default-capacity:100}")
    private int defaultCapacity;

    @Value("${rate-limiter.default-refill-tokens:10}")
    private int defaultRefillTokens;

    @Value("${rate-limiter.default-refill-duration:60}")
    private int defaultRefillDuration;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Creates a Redis template for storing rate limit data.
     *
     * @param connectionFactory the Redis connection factory
     * @return the Redis template
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Gets a bucket for a specific key.
     * If the bucket doesn't exist, it's created with default settings.
     *
     * @param key the bucket key
     * @return the bucket
     */
    public Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createBucket(defaultCapacity, defaultRefillTokens, defaultRefillDuration));
    }

    /**
     * Gets a bucket for a specific key with custom settings.
     *
     * @param key the bucket key
     * @param capacity the bucket capacity
     * @param refillTokens the number of tokens to refill
     * @param refillDuration the refill duration in seconds
     * @return the bucket
     */
    public Bucket getBucket(String key, int capacity, int refillTokens, int refillDuration) {
        return buckets.computeIfAbsent(key, k -> createBucket(capacity, refillTokens, refillDuration));
    }

    /**
     * Creates a new bucket with the specified settings.
     *
     * @param capacity the bucket capacity
     * @param refillTokens the number of tokens to refill
     * @param refillDuration the refill duration in seconds
     * @return the created bucket
     */
    private Bucket createBucket(int capacity, int refillTokens, int refillDuration) {
        Refill refill = Refill.intervally(refillTokens, Duration.ofSeconds(refillDuration));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit)
                .build();
        return Bucket4j.builder().addLimit(limit).build();
    }
}
