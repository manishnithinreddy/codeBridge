package com.codebridge.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for logout requests.
 */
public class LogoutRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public LogoutRequest() {
    }

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

