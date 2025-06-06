package com.codebridge.session.security;

import com.codebridge.session.config.IncomingUserJwtConfigProperties;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
@EnableMethodSecurity(prePostEnabled = true) // For @PreAuthorize etc. if needed later
public class SessionServiceSecurityConfig {

    private final IncomingUserJwtConfigProperties userJwtConfigProperties;

    public SessionServiceSecurityConfig(IncomingUserJwtConfigProperties userJwtConfigProperties) {
        this.userJwtConfigProperties = userJwtConfigProperties;
    }

    // SecurityFilterChain for validating INCOMING USER JWTs (e.g., for /api/lifecycle/** endpoints)
    // This assumes User JWTs are passed in Authorization header for these calls.
    @Bean
    @Order(1) // Define order if multiple SecurityFilterChains exist
    public SecurityFilterChain userJwtFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // This filter chain will apply to paths that need User JWT validation
            // Other paths (like /ops/** which use SessionService's own JWTs in path) might be handled differently
            // or have their security implicitly handled by token validation in controller.
            // For simplicity in restoration, let's assume /api/lifecycle/** is where User JWT is primary.
            // All other requests that don't match a more specific filter chain could be denied or have different rules.
            // The prompt suggests: .anyRequest().authenticated() after permitAll for public paths.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                // Secure lifecycle endpoints with User JWT validation
                .requestMatchers("/api/lifecycle/**").authenticated()
                // For /ops/** endpoints, authentication is primarily via the sessionToken in the path.
                // If we want to also require a User JWT for /ops/**, they'd also be .authenticated().
                // For now, let's assume /ops/** are handled by their token-in-path validation logic
                // and do not strictly require an additional User JWT in Authorization header.
                // If they DO, then .anyRequest().authenticated() would cover them.
                // Let's go with .anyRequest().authenticated() for defense in depth, as per prompt's refined decision.
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(userJwtDecoder()) // Use the decoder for User JWTs
                    .jwtAuthenticationConverter(userJwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    @Bean
    @Qualifier("userJwtDecoder") // Qualify if other JwtDecoders exist (e.g., for session tokens if they were opaque)
    public JwtDecoder userJwtDecoder() {
        byte[] secretBytes = userJwtConfigProperties.getSharedSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(secretBytes);
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    @Qualifier("userJwtAuthenticationConverter")
    public JwtAuthenticationConverter userJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Configure for User JWT roles/authorities if they exist and are needed by SessionService
        // grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        // grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        jwtAuthenticationConverter.setPrincipalClaimName("sub"); // 'sub' claim for platformUserId
        return jwtAuthenticationConverter;
    }
}
