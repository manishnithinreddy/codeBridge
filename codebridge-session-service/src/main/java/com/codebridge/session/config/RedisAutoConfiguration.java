package com.codebridge.session.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to conditionally enable/disable Redis
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
@Import(RedisConfig.class)
public class RedisAutoConfiguration {
    // This class conditionally imports RedisConfig based on the property spring.data.redis.enabled
}

