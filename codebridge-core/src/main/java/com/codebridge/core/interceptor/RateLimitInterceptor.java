package com.codebridge.core.interceptor;

import com.codebridge.core.config.LocalRateLimiterConfig;
import com.codebridge.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * Interceptor for rate limiting.
 * Applies rate limits based on user ID or IP address.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";
    private static final String RATE_LIMIT_RETRY_AFTER_HEADER = "X-Rate-Limit-Retry-After-Seconds";

    private final LocalRateLimiterConfig rateLimiterConfig;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RateLimitInterceptor(LocalRateLimiterConfig rateLimiterConfig, ObjectMapper objectMapper, String keyPrefix) {
        this.rateLimiterConfig = rateLimiterConfig;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = resolveKey(request);
        Bucket bucket = rateLimiterConfig.getBucket(key);
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        // Add rate limit headers
        response.addHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
        
        if (!probe.isConsumed()) {
            // Rate limit exceeded
            response.addHeader(RATE_LIMIT_RETRY_AFTER_HEADER, String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            String traceId = MDC.get(CORRELATION_ID_MDC_KEY);
            
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Rate limit exceeded. Please try again later.",
                    request.getRequestURI(),
                    LocalDateTime.now(),
                    traceId,
                    "RATE_LIMIT_EXCEEDED"
            );
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            
            logger.warn("Rate limit exceeded for key: {}", key);
            
            return false;
        }
        
        return true;
    }

    /**
     * Resolves the rate limit key for a request.
     * Uses user ID if authenticated, or IP address otherwise.
     *
     * @param request the HTTP request
     * @return the rate limit key
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Use user ID for authenticated users
            return keyPrefix + ":user:" + authentication.getName();
        } else {
            // Use IP address for unauthenticated users
            String ipAddress = request.getRemoteAddr();
            return keyPrefix + ":ip:" + ipAddress;
        }
    }
}
