package com.codebridge.aidb.config;

import com.codebridge.aidb.agent.DbSessionMetadata;
import com.codebridge.aidb.model.SessionKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, SessionKey> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SessionKey.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, DbSessionMetadata> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(DbSessionMetadata.class));
        return template;
    }
}

