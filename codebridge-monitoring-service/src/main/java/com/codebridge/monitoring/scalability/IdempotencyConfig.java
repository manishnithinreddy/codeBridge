package com.codebridge.monitoring.scalability.config;

import com.codebridge.monitoring.scalability.service.IdempotencyService;
import com.codebridge.monitoring.scalability.service.impl.HazelcastIdempotencyService;
import com.codebridge.monitoring.scalability.service.impl.JdbcIdempotencyService;
import com.codebridge.monitoring.scalability.service.impl.RedisIdempotencyService;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for idempotency support.
 * Provides idempotency service implementation based on the configured storage type.
 */
@Configuration
@RequiredArgsConstructor
public class IdempotencyConfig {

    @Value("${codebridge.scalability.idempotency.storage-type}")
    private String storageType;

    @Value("${codebridge.scalability.idempotency.expiration-hours}")
    private int expirationHours;

    private final RedisTemplate<String, Object> redisTemplate;
    private final HazelcastInstance hazelcastInstance;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the appropriate idempotency service based on the configured storage type.
     *
     * @return the idempotency service
     */
    @Bean
    public IdempotencyService idempotencyService() {
        switch (storageType.toLowerCase()) {
            case "redis":
                return new RedisIdempotencyService(redisTemplate, expirationHours);
            case "hazelcast":
                return new HazelcastIdempotencyService(hazelcastInstance, expirationHours);
            case "jdbc":
                return new JdbcIdempotencyService(jdbcTemplate, expirationHours);
            default:
                throw new IllegalArgumentException("Unsupported idempotency storage type: " + storageType);
        }
    }
}

