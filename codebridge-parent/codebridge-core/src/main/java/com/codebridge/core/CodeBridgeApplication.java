package com.codebridge.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.codebridge"})
@EntityScan(basePackages = {"com.codebridge.common.model", "com.codebridge.core.model"})
@EnableJpaRepositories(basePackages = {"com.codebridge.core.repository"})
public class CodeBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeBridgeApplication.class, args);
    }
}

