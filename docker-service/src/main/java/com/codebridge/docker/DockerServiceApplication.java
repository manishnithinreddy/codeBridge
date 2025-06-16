package com.codebridge.docker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DockerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerServiceApplication.class, args);
    }
}

