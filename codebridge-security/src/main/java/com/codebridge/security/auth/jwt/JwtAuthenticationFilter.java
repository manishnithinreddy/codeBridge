package com.codebridge.security.auth.jwt;

import com.codebridge.security.audit.AuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter for JWT authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final AuditLogger auditLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String jwt = resolveToken(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Authentication auth = tokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                // Log successful authentication
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("username", auth.getName());
                metadata.put("method", request.getMethod());
                metadata.put("uri", request.getRequestURI());
                metadata.put("remoteAddr", request.getRemoteAddr());
                
                auditLogger.logSecurityEvent(
                        "JWT_AUTHENTICATION_SUCCESS",
                        "User authenticated via JWT",
                        metadata
                );
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
            
            // Log failed authentication
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("method", request.getMethod());
            metadata.put("uri", request.getRequestURI());
            metadata.put("remoteAddr", request.getRemoteAddr());
            metadata.put("errorMessage", e.getMessage());
            
            auditLogger.logSecurityEvent(
                    "JWT_AUTHENTICATION_FAILURE",
                    "Failed to authenticate user via JWT",
                    metadata
            );
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the JWT token from the request.
     *
     * @param request The HTTP request
     * @return The JWT token, or null if not found
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

