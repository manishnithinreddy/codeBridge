package com.codebridge.server.config;

import com.codebridge.server.scalability.autoscaling.AutoScalingService;
import com.codebridge.server.scalability.loadbalancer.LoadBalancerService;
import com.codebridge.server.scalability.resilience.DataResilienceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableScheduling
public class ScalabilityConfig {

    @Value("${codebridge.scalability.load-balancing.strategy:round-robin}")
    private String loadBalancingStrategy;
    
    @Value("${codebridge.scalability.auto-scaling.enabled:true}")
    private boolean autoScalingEnabled;
    
    @Value("${codebridge.scalability.data-resilience.replication.enabled:true}")
    private boolean dataResilienceEnabled;
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * Configure Redis template for distributed data
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure Redis cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("servers", config.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("users", config.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("sessions", config.entryTtl(Duration.ofMinutes(2)))
                .build();
    }

    /**
     * Configure Redisson client for distributed locks and objects
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer()
                  .setAddress(address)
                  .setPassword(redisPassword);
        } else {
            config.useSingleServer()
                  .setAddress(address);
        }
        
        return Redisson.create(config);
    }

    /**
     * Configure Hazelcast for distributed caching
     */
    @Bean
    public HazelcastInstance hazelcastInstance() {
        com.hazelcast.config.Config config = new com.hazelcast.config.Config();
        config.setInstanceName("codebridge-server");
        
        // Configure server cache
        MapConfig serverCache = new MapConfig("serverCache");
        serverCache.setTimeToLiveSeconds(600);
        serverCache.setMaxIdleSeconds(300);
        config.addMapConfig(serverCache);
        
        // Configure session cache
        MapConfig sessionCache = new MapConfig("sessionCache");
        sessionCache.setTimeToLiveSeconds(300);
        sessionCache.setMaxIdleSeconds(120);
        config.addMapConfig(sessionCache);
        
        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Configure load balancer service
     */
    @Bean
    public LoadBalancerService loadBalancerService() {
        return new LoadBalancerService(loadBalancingStrategy);
    }

    /**
     * Configure auto-scaling service
     */
    @Bean
    public AutoScalingService autoScalingService() {
        return new AutoScalingService(autoScalingEnabled);
    }

    /**
     * Configure data resilience service
     */
    @Bean
    public DataResilienceService dataResilienceService() {
        return new DataResilienceService(dataResilienceEnabled);
    }
}

