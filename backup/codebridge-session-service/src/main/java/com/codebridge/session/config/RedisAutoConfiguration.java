package com.codebridge.session.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Redis auto-configuration that imports the appropriate Redis configuration
 * based on whether Redis is enabled or not.
 */
@Configuration
public class RedisAutoConfiguration {

    /**
     * Configuration for when Redis is enabled.
     */
    @Configuration
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
    @Import(RedisConfig.class)
    public static class RedisEnabledConfiguration {
    }

    /**
     * Configuration for when Redis is disabled (test mode).
     */
    @Configuration
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
    @Import(TestRedisConfig.class)
    public static class RedisDisabledConfiguration {
    }
}

