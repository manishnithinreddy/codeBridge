package com.codebridge.session.routing;

import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.model.SessionKey;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A router for session-based requests that directs requests to the correct service instance
 * based on the session token. This enables horizontal scaling by ensuring that requests
 * for a specific session are always routed to the instance that owns that session.
 */
@Component
public class SessionRouter {
    private static final Logger logger = LoggerFactory.getLogger(SessionRouter.class);

    private final RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate;
    private final RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate;
    private final RestTemplate restTemplate;
    private final String applicationInstanceId;
    private final Map<String, String> instanceUrlCache = new ConcurrentHashMap<>();
    
    // Metrics
    private final Counter localRoutingCounter;
    private final Counter remoteRoutingCounter;
    private final Counter routingErrorCounter;

    @Autowired
    public SessionRouter(
            RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate,
            RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate,
            RestTemplate restTemplate,
            @Value("${codebridge.instance.id}") String applicationInstanceId,
            MeterRegistry meterRegistry) {
        this.jwtToSessionKeyRedisTemplate = jwtToSessionKeyRedisTemplate;
        this.sessionMetadataRedisTemplate = sessionMetadataRedisTemplate;
        this.restTemplate = restTemplate;
        this.applicationInstanceId = applicationInstanceId;
        
        // Initialize metrics
        this.localRoutingCounter = Counter.builder("session.routing.local")
                .description("Number of requests routed to the local instance")
                .register(meterRegistry);
        this.remoteRoutingCounter = Counter.builder("session.routing.remote")
                .description("Number of requests routed to a remote instance")
                .register(meterRegistry);
        this.routingErrorCounter = Counter.builder("session.routing.error")
                .description("Number of routing errors")
                .register(meterRegistry);
    }

    /**
     * Routes a request to the correct service instance based on the session token.
     * If the session is owned by this instance, the request is handled locally.
     * Otherwise, it is forwarded to the correct instance.
     *
     * @param sessionToken The session token
     * @param path The API path (without the session token)
     * @param method The HTTP method
     * @param requestBody The request body (can be null for GET requests)
     * @param headers The HTTP headers
     * @param responseType The expected response type
     * @param <T> The type of the response
     * @return The response from the correct instance
     */
    public <T> ResponseEntity<T> routeRequest(
            String sessionToken,
            String path,
            HttpMethod method,
            Object requestBody,
            HttpHeaders headers,
            Class<T> responseType) {
        
        // Get the session key from Redis
        String redisKey = "session:ssh:token:" + sessionToken;
        SessionKey sessionKey = jwtToSessionKeyRedisTemplate.opsForValue().get(redisKey);
        if (sessionKey == null) {
            logger.warn("Session key not found for token: {}", sessionToken);
            routingErrorCounter.increment();
            throw new IllegalArgumentException("Invalid session token");
        }
        
        // Get the session metadata from Redis
        String metadataKey = "session:ssh:metadata:" + sessionKey.platformUserId() + ":" + 
                sessionKey.resourceId() + ":" + sessionKey.sessionType();
        SshSessionMetadata metadata = sessionMetadataRedisTemplate.opsForValue().get(metadataKey);
        if (metadata == null) {
            logger.warn("Session metadata not found for key: {}", sessionKey);
            routingErrorCounter.increment();
            throw new IllegalArgumentException("Session metadata not found");
        }
        
        // Check if this instance owns the session
        if (applicationInstanceId.equals(metadata.hostingInstanceId())) {
            // This instance owns the session, so handle the request locally
            logger.debug("Handling request locally for session token: {}", sessionToken);
            localRoutingCounter.increment();
            return null; // Null indicates that the request should be handled locally
        }
        
        // This instance does not own the session, so forward the request to the correct instance
        String targetInstanceUrl = getInstanceUrl(metadata.hostingInstanceId());
        if (targetInstanceUrl == null) {
            logger.error("Target instance URL not found for instance ID: {}", metadata.hostingInstanceId());
            routingErrorCounter.increment();
            throw new IllegalStateException("Target instance URL not found");
        }
        
        // Forward the request to the correct instance
        String targetUrl = UriComponentsBuilder.fromHttpUrl(targetInstanceUrl)
                .path(path.replace("{sessionToken}", sessionToken))
                .build()
                .toUriString();
        
        logger.debug("Forwarding request to: {}", targetUrl);
        remoteRoutingCounter.increment();
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
        return restTemplate.exchange(targetUrl, method, requestEntity, responseType);
    }
    
    /**
     * Gets the URL for a service instance.
     * In a production environment, this would typically be retrieved from a service registry
     * like Eureka, Consul, or Kubernetes Service Discovery.
     *
     * @param instanceId The instance ID
     * @return The URL for the instance
     */
    private String getInstanceUrl(String instanceId) {
        // Check the cache first
        if (instanceUrlCache.containsKey(instanceId)) {
            return instanceUrlCache.get(instanceId);
        }
        
        // In a real implementation, this would look up the instance URL from a service registry
        // For now, we'll use a simple mapping for demonstration purposes
        // In production, this would be replaced with a service discovery mechanism
        
        // TODO: Replace with actual service discovery
        String instanceUrl = "http://" + instanceId + ":8080";
        instanceUrlCache.put(instanceId, instanceUrl);
        return instanceUrl;
    }
    
    /**
     * Clears the instance URL cache.
     * This should be called periodically to ensure that the cache stays up-to-date.
     */
    public void clearInstanceUrlCache() {
        instanceUrlCache.clear();
    }
}

