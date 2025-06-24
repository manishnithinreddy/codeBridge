package com.codebridge.session.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for test mode.
 * This provides no-op Redis implementations when Redis is disabled.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class TestRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new MockRedisConnectionFactory();
    }

    @Bean
    public <T> RedisTemplate<String, T> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * A mock Redis connection factory for test mode.
     * This is a simplified implementation that doesn't actually connect to Redis.
     */
    private static class MockRedisConnectionFactory implements RedisConnectionFactory {
        @Override
        public Object getConnection() {
            // Return null as we're not actually connecting to Redis in test mode
            return null;
        }

        @Override
        public Object getConnection(String s) {
            // Return null as we're not actually connecting to Redis in test mode
            return null;
        }

        @Override
        public RedisConnectionFactory getClusterConnection() {
            return this;
        }

        @Override
        public boolean getConvertPipelineAndTxResults() {
            return false;
        }
    }
}

