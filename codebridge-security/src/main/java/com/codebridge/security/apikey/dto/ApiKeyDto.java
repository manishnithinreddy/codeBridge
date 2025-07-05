package com.codebridge.security.apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO for API keys.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyDto {

    private Long id;
    private String name;
    private String prefix;
    private Long userId;
    private LocalDateTime expirationDate;
    private boolean enabled;
    private LocalDateTime lastUsed;
    private long usageCount;
    private Integer rateLimit;
    private Set<String> scopes = new HashSet<>();
    private Set<String> ipRestrictions = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime revokedAt;
    private Long revokedBy;
    private String revocationReason;

    // Manual builder method
    public static ApiKeyDtoBuilder builder() {
        return new ApiKeyDtoBuilder();
    }

    // Manual builder class
    public static class ApiKeyDtoBuilder {
        private Long id;
        private String name;
        private String prefix;
        private Long userId;
        private LocalDateTime expirationDate;
        private boolean enabled;
        private LocalDateTime lastUsed;
        private long usageCount;
        private Integer rateLimit;
        private Set<String> scopes = new HashSet<>();
        private Set<String> ipRestrictions = new HashSet<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime revokedAt;
        private Long revokedBy;
        private String revocationReason;

        public ApiKeyDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApiKeyDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ApiKeyDtoBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ApiKeyDtoBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ApiKeyDtoBuilder expirationDate(LocalDateTime expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public ApiKeyDtoBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ApiKeyDtoBuilder lastUsed(LocalDateTime lastUsed) {
            this.lastUsed = lastUsed;
            return this;
        }

        public ApiKeyDtoBuilder usageCount(long usageCount) {
            this.usageCount = usageCount;
            return this;
        }

        public ApiKeyDtoBuilder rateLimit(Integer rateLimit) {
            this.rateLimit = rateLimit;
            return this;
        }

        public ApiKeyDtoBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public ApiKeyDtoBuilder ipRestrictions(Set<String> ipRestrictions) {
            this.ipRestrictions = ipRestrictions;
            return this;
        }

        public ApiKeyDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ApiKeyDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ApiKeyDtoBuilder revokedAt(LocalDateTime revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public ApiKeyDtoBuilder revokedBy(Long revokedBy) {
            this.revokedBy = revokedBy;
            return this;
        }

        public ApiKeyDtoBuilder revocationReason(String revocationReason) {
            this.revocationReason = revocationReason;
            return this;
        }

        public ApiKeyDto build() {
            ApiKeyDto dto = new ApiKeyDto();
            dto.id = this.id;
            dto.name = this.name;
            dto.prefix = this.prefix;
            dto.userId = this.userId;
            dto.expirationDate = this.expirationDate;
            dto.enabled = this.enabled;
            dto.lastUsed = this.lastUsed;
            dto.usageCount = this.usageCount;
            dto.rateLimit = this.rateLimit;
            dto.scopes = this.scopes;
            dto.ipRestrictions = this.ipRestrictions;
            dto.createdAt = this.createdAt;
            dto.updatedAt = this.updatedAt;
            dto.revokedAt = this.revokedAt;
            dto.revokedBy = this.revokedBy;
            dto.revocationReason = this.revocationReason;
            return dto;
        }
    }
}
