package com.codebridge.security.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    private String accessToken;
    private String refreshToken;
    private boolean mfaRequired;
    private String mfaToken;
    private Long userId;
    private String username;

    // Manual builder method
    public static AuthenticationResponseBuilder builder() {
        return new AuthenticationResponseBuilder();
    }

    // Manual builder class
    public static class AuthenticationResponseBuilder {
        private String accessToken;
        private String refreshToken;
        private boolean mfaRequired;
        private String mfaToken;
        private Long userId;
        private String username;

        public AuthenticationResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthenticationResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AuthenticationResponseBuilder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public AuthenticationResponseBuilder mfaToken(String mfaToken) {
            this.mfaToken = mfaToken;
            return this;
        }

        public AuthenticationResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AuthenticationResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuthenticationResponse build() {
            AuthenticationResponse response = new AuthenticationResponse();
            response.accessToken = this.accessToken;
            response.refreshToken = this.refreshToken;
            response.mfaRequired = this.mfaRequired;
            response.mfaToken = this.mfaToken;
            response.userId = this.userId;
            response.username = this.username;
            return response;
        }
    }

    // Manual getter methods
    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
