package com.codebridge.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan; // Added
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // Added
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient // If Eureka is used
@EnableScheduling      // For scheduled tasks like session cleanup
@EnableJpaRepositories("com.codebridge.session.repository") // Added: Specify repository package
@EntityScan("com.codebridge.session.model")               // Added: Specify model package
public class SessionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionServiceApplication.class, args);
    }

}
