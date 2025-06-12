package com.codebridge.security.apikey.filter;

import com.codebridge.security.apikey.model.ApiKey;
import com.codebridge.security.apikey.service.ApiKeyService;
import com.codebridge.security.audit.AuditLogger;
import com.codebridge.security.auth.model.User;
import com.codebridge.security.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter for API key authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String apiKey = resolveApiKey(request);
            
            if (StringUtils.hasText(apiKey)) {
                String ipAddress = getClientIpAddress(request);
                ApiKey apiKeyEntity = apiKeyService.validateApiKey(apiKey, ipAddress);
                
                if (apiKeyEntity != null) {
                    // Get user
                    User user = userRepository.findById(apiKeyEntity.getUserId())
                            .orElse(null);
                    
                    if (user != null && user.isEnabled()) {
                        // Create authentication
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities());
                        
                        // Set authentication in context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // Log successful authentication
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("username", user.getUsername());
                        metadata.put("userId", user.getId());
                        metadata.put("apiKeyId", apiKeyEntity.getId());
                        metadata.put("apiKeyName", apiKeyEntity.getName());
                        metadata.put("method", request.getMethod());
                        metadata.put("uri", request.getRequestURI());
                        metadata.put("remoteAddr", ipAddress);
                        
                        auditLogger.logSecurityEvent(
                                "API_KEY_AUTHENTICATION_SUCCESS",
                                "User authenticated via API key",
                                metadata
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
            
            // Log failed authentication
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("method", request.getMethod());
            metadata.put("uri", request.getRequestURI());
            metadata.put("remoteAddr", getClientIpAddress(request));
            metadata.put("errorMessage", e.getMessage());
            
            auditLogger.logSecurityEvent(
                    "API_KEY_AUTHENTICATION_FAILURE",
                    "Failed to authenticate user via API key",
                    metadata
            );
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the API key from the request.
     *
     * @param request The HTTP request
     * @return The API key, or null if not found
     */
    private String resolveApiKey(HttpServletRequest request) {
        // Try to get from header
        String apiKey = request.getHeader("X-API-Key");
        
        // If not in header, try to get from query parameter
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getParameter("api_key");
        }
        
        return apiKey;
    }

    /**
     * Gets the client IP address from the request.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        
        if (StringUtils.hasText(xForwardedFor)) {
            // Get the first IP in the list
            return xForwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }
}

