package com.codebridge.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import com.codebridge.server.config.IncomingUserJwtConfigProperties; // New import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder; // New import
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder; // New import
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec; // New import
import java.nio.charset.StandardCharsets; // New import

/**
 * Security configuration for the Server Management service.
 * Configures JWT-based authentication and authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ServerSecurityConfig {

    private final IncomingUserJwtConfigProperties incomingJwtConfigProperties;

    public ServerSecurityConfig(IncomingUserJwtConfigProperties incomingJwtConfigProperties) {
        this.incomingJwtConfigProperties = incomingJwtConfigProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**", "/public/**").permitAll() // Added /public/**
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder()) // Configure custom decoder
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretKeyBytes = incomingJwtConfigProperties.getSharedSecret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "HMACSHA512"); // Or "HMACSHA256"
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // If ServerRoleConverter is still relevant for User JWTs, keep it. Otherwise, simplify.
        converter.setJwtGrantedAuthoritiesConverter(new ServerRoleConverter());
        converter.setPrincipalClaimName("sub"); // Ensure "sub" claim is used for Authentication.getName()
        return converter;
    }
}

