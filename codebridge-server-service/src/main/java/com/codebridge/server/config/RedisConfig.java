package com.codebridge.server.config;

import com.codebridge.server.dto.sessions.DbSessionMetadata;
import com.codebridge.server.dto.sessions.SshSessionMetadata;
import com.codebridge.server.sessions.SessionKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import java.time.Duration;
// Map and HashMap for per-cache config example, not used in default config
// import java.util.HashMap;
// import java.util.Map;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching // Enable Spring's caching capabilities
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // For Java 8 Date/Time types if SessionKey or Metadata were to use them
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // It's good practice to enable default typing if dealing with polymorphic types,
        // but for specific DTOs like SessionKey and SshSessionMetadata, it might not be strictly necessary
        // if the RedisTemplate is typed. For broader use, enable it:
        // objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, SessionKey> sessionKeyRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, SessionKey> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        // For Hash operations, if needed:
        // template.setHashKeySerializer(new StringRedisSerializer());
        // template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    @Bean
    public RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
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

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        // Use the existing 'redisObjectMapper' bean for consistency
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)) // Default TTL for cache entries: 10 minutes
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
            .disableCachingNullValues(); // Optional: prevent caching of null values

        // Example for per-cache configuration (can be added later if needed by enabling below):
        // Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // cacheConfigurations.put("sshKeyById", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        // cacheConfigurations.put("serverById", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            // .withInitialCacheConfigurations(cacheConfigurations) // If using per-cache configs
            .build();
    }
}
