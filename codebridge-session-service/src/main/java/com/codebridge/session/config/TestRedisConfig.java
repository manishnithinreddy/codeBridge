package com.codebridge.session.config;

import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.model.SessionKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // Create a mock Redis connection factory for testing
        return new LettuceConnectionFactory();
    }

    @Bean
    @Primary
    public RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, SessionKey> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SessionKey.class));
        return template;
    }

    @Bean
    @Primary
    public RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, SshSessionMetadata> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SshSessionMetadata.class));
        return template;
    }
}

