package com.codebridge.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
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

    @Value("${codebridge.gateway.security.public-paths}")
    private List<String> publicPaths;

    /**
     * Configures the security filter chain for the API Gateway.
     *
     * @param http The server HTTP security
     * @return The security filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Convert public paths to an array
        String[] publicPathsArray = publicPaths.toArray(new String[0]);
        
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(publicPathsArray).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))
                        )
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

    /**
     * Creates a JWT authentication converter for extracting user details from JWT tokens.
     *
     * @return The JWT authentication converter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Configure JWT claims to authorities mapping if needed
        // For example, to map roles from the "roles" claim:
        // JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // authoritiesConverter.setAuthoritiesClaimName("roles");
        // authoritiesConverter.setAuthorityPrefix("ROLE_");
        // converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        return converter;
    }
}

