package com.codebridge.session.config; // Adapted package

import com.codebridge.session.dto.DbSessionMetadata; // Adapted import
import com.codebridge.session.dto.SshSessionMetadata; // Adapted import
import com.codebridge.session.model.SessionKey; // Adapted import
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// Spring Cache related imports removed as SessionService might not use @Cacheable directly
// import org.springframework.cache.CacheManager;
// import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.serializer.RedisSerializationContext;
// import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
// @EnableCaching // Removed for SessionService unless it uses @Cacheable internally
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() { // Bean name can be same or specific
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, SessionKey> sessionKeyRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, SessionKey> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    @Bean
    public RedisTemplate<String, SshSessionMetadata> sshSessionMetadataRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, SshSessionMetadata> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    @Bean
    public RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, DbSessionMetadata> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    // CacheManager bean removed for SessionService as it primarily uses Redis directly for session state,
    // not necessarily for Spring's @Cacheable method caching on its own services.
    // If SessionService develops internal methods that would benefit from @Cacheable,
    // then @EnableCaching and CacheManager bean should be added here.
}
