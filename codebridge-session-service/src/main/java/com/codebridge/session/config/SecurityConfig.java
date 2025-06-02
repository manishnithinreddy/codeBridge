package com.codebridge.session.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections; // For empty authorities list

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final IncomingJwtConfigProperties incomingJwtConfigProperties;

    public SecurityConfig(IncomingJwtConfigProperties incomingJwtConfigProperties) {
        this.incomingJwtConfigProperties = incomingJwtConfigProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF as we are using token-based auth
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless sessions
            .authorizeHttpRequests(auth -> auth
                // .requestMatchers("/public/**").permitAll() // Example for public endpoints
                .anyRequest().authenticated() // All other requests require authentication
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Using HMAC SHA-256/384/512, algorithm choice depends on key length and IdP configuration
        // For HS256, key should be >= 32 bytes. For HS512, >= 64 bytes.
        byte[] secretKeyBytes = incomingJwtConfigProperties.getSharedSecret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "HMACSHA512"); // Or "HMACSHA256"
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Configure if authorities are in a specific claim, e.g., "roles" or "scope"
        // For now, no specific authorities are extracted beyond the principal.
        // grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        // grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // if roles don't start with ROLE_

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> Collections.emptyList()); // No app-specific authorities from JWT for now

        // The principal will be the Jwt object itself by default with JwtAuthenticationToken.
        // To make Authentication.getName() return the platformUserId (subject claim):
        jwtAuthenticationConverter.setPrincipalClaimName("sub");

        return jwtAuthenticationConverter;
    }
}
