package com.codebridge.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.codebridge"})
@EntityScan(basePackages = {"com.codebridge.core.model", "com.codebridge.common.model"})
@EnableJpaRepositories(basePackages = {"com.codebridge.core.repository"})
public class CodeBridgeCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeBridgeCoreApplication.class, args);
    }
}

