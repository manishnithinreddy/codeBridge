package com.codebridge.aidb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // For Eureka client
public class AiDbAgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDbAgentServiceApplication.class, args);
    }

}
