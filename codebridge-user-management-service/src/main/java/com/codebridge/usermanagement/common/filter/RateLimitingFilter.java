package com.codebridge.usermanagement.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter that implements rate limiting for API requests.
 */
@Component
@Order(2)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rate-limiting.enabled:true}")
    private boolean enabled;

    @Value("${rate-limiting.default-limit:100}")
    private int defaultLimit;

    @Value("${rate-limiting.ip-limit:50}")
    private int ipLimit;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientIdentifier(request);
        Bucket bucket = buckets.computeIfAbsent(clientId, this::createBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for client: {}", clientId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please try again later."
            );
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    /**
     * Creates a new rate limiting bucket for a client.
     *
     * @param clientId the client identifier
     * @return the rate limiting bucket
     */
    private Bucket createBucket(String clientId) {
        // Determine if this is an IP-based limit or a user-based limit
        int limit = clientId.startsWith("ip:") ? ipLimit : defaultLimit;
        
        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.greedy(limit, Duration.ofMinutes(1)));
        return Bucket4j.builder().addLimit(bandwidth).build();
    }

    /**
     * Gets the client identifier for rate limiting.
     * Uses the authenticated user ID if available, otherwise falls back to the client IP.
     *
     * @param request the HTTP request
     * @return the client identifier
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // If user is authenticated, use their ID
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName();
        }
        
        // Otherwise, use the client IP
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        return "ip:" + clientIp;
    }
}

