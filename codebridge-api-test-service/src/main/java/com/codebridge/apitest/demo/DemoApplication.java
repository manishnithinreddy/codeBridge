package com.codebridge.apitest.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Demo application to showcase the API Test Service features.
 * This application runs with an in-memory H2 database and demonstrates
 * the key features of the API Test Service.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.codebridge.apitest")
@EntityScan(basePackages = "com.codebridge.apitest.model")
@EnableJpaRepositories(basePackages = "com.codebridge.apitest.repository")
public class DemoApplication {

    public static void main(String[] args) {
        // Set the demo profile
        System.setProperty("spring.profiles.active", "demo");
        
        // Run the application
        SpringApplication.run(DemoApplication.class, args);
    }
}

