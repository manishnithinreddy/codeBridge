package com.codebridge.session.config;

import com.codebridge.session.service.CustomJschHostKeyRepository;
import com.jcraft.jsch.JSch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for SSH connection components.
 * This helps prepare the system for scaling by providing centralized configuration for connection management.
 */
@Configuration
public class ConnectionConfig {

    private final CustomJschHostKeyRepository customJschHostKeyRepository;

    @Autowired
    public ConnectionConfig(CustomJschHostKeyRepository customJschHostKeyRepository) {
        this.customJschHostKeyRepository = customJschHostKeyRepository;
    }

    /**
     * Creates and configures a JSch instance for SSH connections.
     *
     * @return A configured JSch instance
     */
    @Bean
    public JSch jsch() {
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(customJschHostKeyRepository);
        return jsch;
    }
}

