package com.codebridge.gateway.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

import java.util.List;

/**
 * Security configuration for the API Gateway.
 * Configures authentication, authorization, and security headers.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for the API Gateway.
     *
     * @param http The server HTTP security
     * @return The security filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Define public paths that don't require authentication
        String[] publicPathsArray = {
            "/actuator/**",
            "/eureka/**", 
            "/health",
            "/info",
            "/prometheus",
            "/metrics",
            "/auth/login",
            "/auth/register",
            "/api/public/**",
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/images/**"
        };
        
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(publicPathsArray).permitAll()
                        .anyExchange().permitAll() // Allow all for now, can be configured later
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions
                                .mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN)
                        )
                        .cache(cache -> cache.disable())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'self'; form-action 'self'; script-src 'self' 'unsafe-inline'; img-src 'self' data:; style-src 'self' 'unsafe-inline';")
                        )
                )
                .build();
    }


}
