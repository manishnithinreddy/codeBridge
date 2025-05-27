package com.codebridge.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the User Management Service.
 * This service handles user profiles, preferences, settings, team memberships, and feature flags.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.codebridge.usermanagement"})
public class UserManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementServiceApplication.class, args);
    }
}
