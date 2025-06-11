package com.codebridge.session.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis configuration for test mode.
 * This provides no-op Redis implementations when Redis is disabled.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class TestRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new NoOpRedisConnectionFactory();
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
     * A no-op Redis connection factory for test mode.
     */
    private static class NoOpRedisConnectionFactory implements RedisConnectionFactory {
        @Override
        public RedisConnection getConnection() {
            return new NoOpRedisConnection();
        }

        @Override
        public RedisConnection getConnection(String s) {
            return new NoOpRedisConnection();
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

    /**
     * A no-op Redis connection for test mode.
     */
    private static class NoOpRedisConnection implements RedisConnection {
        private final ConcurrentHashMap<byte[], byte[]> dataStore = new ConcurrentHashMap<>();

        @Override
        public void close() {
            // No-op
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public Object getNativeConnection() {
            return null;
        }

        // Implement other methods as needed for your tests
        // For example:
        
        @Override
        public Boolean exists(byte[] key) {
            return dataStore.containsKey(key);
        }
        
        @Override
        public Long del(byte[]... keys) {
            long count = 0;
            for (byte[] key : keys) {
                if (dataStore.remove(key) != null) {
                    count++;
                }
            }
            return count;
        }
        
        @Override
        public byte[] get(byte[] key) {
            return dataStore.get(key);
        }
        
        @Override
        public Boolean set(byte[] key, byte[] value) {
            dataStore.put(key, value);
            return true;
        }
        
        @Override
        public Boolean set(byte[] key, byte[] value, long expiration, TimeUnit unit) {
            dataStore.put(key, value);
            // Ignore expiration in this simple implementation
            return true;
        }
        
        // Add other methods as needed
    }
}

