package com.codebridge.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

