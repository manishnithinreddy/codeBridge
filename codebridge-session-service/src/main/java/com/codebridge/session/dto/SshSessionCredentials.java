package com.codebridge.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public class SshSessionCredentials implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Host cannot be blank")
    private String host;

    @NotNull(message = "Port cannot be null")
    @Positive(message = "Port must be a positive number")
    private Integer port = 22; // Default SSH port

    @NotBlank(message = "Username cannot be blank")
    private String username;

    // Password can be blank if using private key
    private String password;

    // Private key can be blank if using password
    private String privateKey;

    // Constructors
    public SshSessionCredentials() {}

    // Getters and Setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}

