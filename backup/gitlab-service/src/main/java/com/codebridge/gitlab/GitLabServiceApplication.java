package com.codebridge.gitlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GitLabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitLabServiceApplication.class, args);
    }
}

