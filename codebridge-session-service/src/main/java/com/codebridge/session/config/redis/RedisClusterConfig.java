package com.codebridge.session.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Redis Cluster configuration for scaling to support large numbers of concurrent sessions.
 * This configuration is only active when the "scaling" profile is active.
 */
@Configuration
@Profile("scaling")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes:localhost:6379}")
    private List<String> clusterNodes;

    @Value("${spring.redis.cluster.max-redirects:3}")
    private Integer maxRedirects;

    @Value("${spring.redis.timeout:2000}")
    private Integer timeout;

    @Value("${spring.redis.lettuce.shutdown-timeout:100}")
    private Integer shutdownTimeout;

    /**
     * Creates a Redis connection factory configured for cluster mode.
     *
     * @return A LettuceConnectionFactory configured for Redis Cluster
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(clusterNodes);
        clusterConfiguration.setMaxRedirects(maxRedirects);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .shutdownTimeout(Duration.ofMillis(shutdownTimeout))
                .build();

        return new LettuceConnectionFactory(clusterConfiguration, clientConfig);
    }

    /**
     * Creates a Redis template for session key objects.
     *
     * @param connectionFactory The Redis connection factory
     * @return A configured RedisTemplate
     */
    @Bean
    public <T> RedisTemplate<String, T> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

