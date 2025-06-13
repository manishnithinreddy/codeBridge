package com.codebridge.server.security;

import com.codebridge.server.config.IncomingUserJwtConfigProperties;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ServerSecurityConfig {

    private final IncomingUserJwtConfigProperties jwtConfigProperties;

    public ServerSecurityConfig(IncomingUserJwtConfigProperties jwtConfigProperties) {
        this.jwtConfigProperties = jwtConfigProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/server/health"
                ).permitAll()
                .anyRequest().permitAll() // For test profile, allow all requests
            );
        
        // Only configure OAuth2 resource server for non-test profiles
        if (!isTestProfile()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    try {
                        jwt.decoder(jwtDecoder());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure JWT decoder", e);
                    }
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                })
            );
        }
        
        // http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // For H2 console

        return http.build();
    }
    
    private boolean isTestProfile() {
        // Simple check for test profile - in a real app, use Environment or similar
        return System.getProperty("spring.profiles.active", "").contains("test") || 
               System.getenv("SPRING_PROFILES_ACTIVE") != null && 
               System.getenv("SPRING_PROFILES_ACTIVE").contains("test");
    }

    @Bean
    @Profile("!test")
    public JwtDecoder jwtDecoder() {
        byte[] secretBytes = jwtConfigProperties.getSharedSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(secretBytes);
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // If User JWTs have roles in a "roles" or "authorities" claim, configure here:
        // grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        // grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // Or "" if no prefix

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        // Set the principal name to be the 'sub' claim (platformUserId)
        jwtAuthenticationConverter.setPrincipalClaimName("sub");
        return jwtAuthenticationConverter;
    }
}

