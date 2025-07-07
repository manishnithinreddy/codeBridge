package com.codebridge.monitoring.scalability.config;

import com.codebridge.monitoring.scalability.model.SessionStoreType;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Duration;

/**
 * Configuration for distributed session management.
 * Supports multiple session store types: Redis, Hazelcast, and JDBC.
 */
@Configuration
@RequiredArgsConstructor
public class SessionConfig {

    @Value("${codebridge.scalability.session.store-type}")
    private String sessionStoreType;

    @Value("${codebridge.scalability.session.cookie-name}")
    private String cookieName;

    @Value("${codebridge.scalability.session.timeout-seconds}")
    private int timeoutSeconds;

    private final HazelcastInstance hazelcastInstance;
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Configure the cookie serializer for session management.
     *
     * @return the cookie serializer
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName);
        serializer.setCookieMaxAge(timeoutSeconds);
        serializer.setCookiePath("/");
        serializer.setUseSecureCookie(true);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }

    /**
     * Configure the Redis session repository customizer.
     *
     * @return the session repository customizer
     */
    @Bean
    public SessionRepositoryCustomizer<RedisSessionRepository> redisSessionRepositoryCustomizer() {
        return repository -> {
            repository.setDefaultMaxInactiveInterval(Duration.ofSeconds(timeoutSeconds));
            repository.setFlushMode(FlushMode.IMMEDIATE);
            repository.setSaveMode(SaveMode.ALWAYS);
        };
    }

    /**
     * Configure the Hazelcast session repository customizer.
     *
     * @return the session repository customizer
     */
    @Bean
    public SessionRepositoryCustomizer<HazelcastIndexedSessionRepository> hazelcastSessionRepositoryCustomizer() {
        return repository -> {
            repository.setDefaultMaxInactiveInterval(Duration.ofSeconds(timeoutSeconds));
            repository.setFlushMode(FlushMode.IMMEDIATE);
            repository.setSaveMode(SaveMode.ALWAYS);
        };
    }

    /**
     * Configure the Redis template for session management.
     *
     * @return the Redis template
     */
    @Bean
    public RedisTemplate<String, Object> sessionRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * Enable the appropriate session repository based on the configured store type.
     */
    @Configuration
    public class SessionRepositoryConfig {

        @Configuration
        @EnableRedisHttpSession
        @ConditionalOnSessionStoreType(SessionStoreType.REDIS)
        public class RedisSessionConfig {
        }

        @Configuration
        @EnableHazelcastHttpSession
        @ConditionalOnSessionStoreType(SessionStoreType.HAZELCAST)
        public class HazelcastSessionConfig {
        }

        @Configuration
        @EnableJdbcHttpSession
        @ConditionalOnSessionStoreType(SessionStoreType.JDBC)
        public class JdbcSessionConfig {
        }
    }
}
