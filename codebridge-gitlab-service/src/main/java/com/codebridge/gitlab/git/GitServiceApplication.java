package com.codebridge.git;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GitServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitServiceApplication.class, args);
    }
}

