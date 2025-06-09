package com.codebridge.session.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * Security configuration for WebSocket connections.
 */
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
                // Allow all connections
                .simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
                // Allow subscription to public topics
                .simpDestMatchers("/topic/**").permitAll()
                // Require authentication for user-specific destinations
                .simpDestMatchers("/user/**").authenticated()
                // Require authentication for application destinations
                .simpDestMatchers("/app/**").authenticated()
                // Deny all other messages by default
                .anyMessage().denyAll()
                .build();
    }
}

