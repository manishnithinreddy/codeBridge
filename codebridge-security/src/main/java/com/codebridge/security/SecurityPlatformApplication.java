package com.codebridge.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the CodeBridge Security Platform.
 * This consolidated service combines the functionality of the previous
 * Security service and Identity Platform service.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.codebridge.security",
    "com.codebridge.security.identity"
})
public class SecurityPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityPlatformApplication.class, args);
    }
}

