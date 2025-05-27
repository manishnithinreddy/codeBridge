package com.codebridge.usermanagement.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that adds security headers to HTTP responses.
 */
@Component
@Order(3)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${security.headers.content-security-policy:default-src 'self'; frame-ancestors 'none'; form-action 'self'}")
    private String contentSecurityPolicy;

    @Value("${security.headers.strict-transport-security:max-age=31536000; includeSubDomains}")
    private String strictTransportSecurity;

    @Value("${security.headers.x-content-type-options:nosniff}")
    private String xContentTypeOptions;

    @Value("${security.headers.x-frame-options:DENY}")
    private String xFrameOptions;

    @Value("${security.headers.x-xss-protection:1; mode=block}")
    private String xXssProtection;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Add security headers
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        response.setHeader("Strict-Transport-Security", strictTransportSecurity);
        response.setHeader("X-Content-Type-Options", xContentTypeOptions);
        response.setHeader("X-Frame-Options", xFrameOptions);
        response.setHeader("X-XSS-Protection", xXssProtection);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Feature-Policy", "camera 'none'; microphone 'none'; geolocation 'none'");
        
        filterChain.doFilter(request, response);
    }
}

