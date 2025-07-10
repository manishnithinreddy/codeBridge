package com.codebridge.gitlab.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the GitLab Integration service.
 * Configures JWT-based authentication and authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class GitLabSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    /**
     * Configures the security filter chain.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().permitAll() // Allow all requests for development
            )
            // Temporarily disable OAuth2 for development
            // .oauth2ResourceServer(oauth2 -> oauth2
            //     .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            // )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }

    // JWT decoder temporarily disabled for development
    // @Bean
    // public JwtDecoder jwtDecoder() {
    //     if (jwkSetUri != null && !jwkSetUri.isEmpty()) {
    //         return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    //     } else {
    //         // For development/testing - create a decoder that accepts any JWT
    //         // In production, this should be properly configured with a real JWK Set URI
    //         return NimbusJwtDecoder.withJwkSetUri("http://localhost:8080/.well-known/jwks.json").build();
    //     }
    // }

    // JWT authentication converter temporarily disabled for development
    // @Bean
    // public JwtAuthenticationConverter jwtAuthenticationConverter() {
    //     JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    //     converter.setJwtGrantedAuthoritiesConverter(new GitLabRoleConverter());
    //     return converter;
    // }
}
