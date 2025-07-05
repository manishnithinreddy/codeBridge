package com.codebridge.security.apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO for API key responses.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyResponse {

    private Long id;
    private String name;
    private String apiKey;
    private String prefix;
    private LocalDateTime expirationDate;
    private Set<String> scopes = new HashSet<>();
    private Set<String> ipRestrictions = new HashSet<>();
    private Integer rateLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Manual builder method
    public static ApiKeyResponseBuilder builder() {
        return new ApiKeyResponseBuilder();
    }

    // Manual builder class
    public static class ApiKeyResponseBuilder {
        private Long id;
        private String name;
        private String apiKey;
        private String prefix;
        private LocalDateTime expirationDate;
        private Set<String> scopes = new HashSet<>();
        private Set<String> ipRestrictions = new HashSet<>();
        private Integer rateLimit;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ApiKeyResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApiKeyResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ApiKeyResponseBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public ApiKeyResponseBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ApiKeyResponseBuilder expirationDate(LocalDateTime expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public ApiKeyResponseBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public ApiKeyResponseBuilder ipRestrictions(Set<String> ipRestrictions) {
            this.ipRestrictions = ipRestrictions;
            return this;
        }

        public ApiKeyResponseBuilder rateLimit(Integer rateLimit) {
            this.rateLimit = rateLimit;
            return this;
        }

        public ApiKeyResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ApiKeyResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ApiKeyResponse build() {
            ApiKeyResponse response = new ApiKeyResponse();
            response.id = this.id;
            response.name = this.name;
            response.apiKey = this.apiKey;
            response.prefix = this.prefix;
            response.expirationDate = this.expirationDate;
            response.scopes = this.scopes;
            response.ipRestrictions = this.ipRestrictions;
            response.rateLimit = this.rateLimit;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
        }
    }
}
