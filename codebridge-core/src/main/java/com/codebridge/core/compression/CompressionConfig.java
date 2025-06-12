package com.codebridge.core.compression;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for request and response compression.
 */
@Configuration
public class CompressionConfig {

    @Value("${codebridge.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${codebridge.compression.min-response-size:2048}")
    private int minResponseSize;

    @Value("${codebridge.compression.mime-types:application/json,application/xml,text/html,text/xml,text/plain}")
    private String[] mimeTypes;

    /**
     * Customizes the web server factory to enable compression.
     *
     * @return The web server factory customizer
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> compressionCustomizer() {
        return factory -> {
            Compression compression = new Compression();
            compression.setEnabled(compressionEnabled);
            compression.setMinResponseSize(minResponseSize);
            compression.setMimeTypes(mimeTypes);
            factory.setCompression(compression);
        };
    }
}

