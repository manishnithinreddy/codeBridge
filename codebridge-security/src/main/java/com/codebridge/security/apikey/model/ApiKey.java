package com.codebridge.security.apikey.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * API key entity.
 */
@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String keyPrefix;

    @Column(nullable = false)
    private String keyHash;

    @Column(nullable = false)
    private String salt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "usage_count")
    private long usageCount = 0;

    @Column(name = "rate_limit")
    private Integer rateLimit;

    @ElementCollection
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope")
    private Set<String> scopes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "api_key_ip_restrictions", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "ip_address")
    private Set<String> ipRestrictions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(name = "revocation_reason")
    private String revocationReason;

    /**
     * Checks if the API key is expired.
     *
     * @return True if the API key is expired, false otherwise
     */
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    /**
     * Checks if the API key is valid.
     *
     * @return True if the API key is valid, false otherwise
     */
    public boolean isValid() {
        return enabled && !isExpired() && revokedAt == null;
    }

    /**
     * Revokes the API key.
     *
     * @param revokedBy The user ID who revoked the API key
     * @param reason The revocation reason
     */
    public void revoke(Long revokedBy, String reason) {
        this.enabled = false;
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = revokedBy;
        this.revocationReason = reason;
    }

    /**
     * Records API key usage.
     */
    public void recordUsage() {
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
    }

    // Manual getters for fields that Lombok might not be generating properly
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Set<String> getIpRestrictions() {
        return ipRestrictions;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public String getName() {
        return name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public Long getId() {
        return id;
    }

    public void setIpRestrictions(Set<String> ipRestrictions) {
        this.ipRestrictions = ipRestrictions;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getSalt() {
        return salt;
    }

    // Manual builder method
    public static ApiKeyBuilder builder() {
        return new ApiKeyBuilder();
    }

    // Manual builder class
    public static class ApiKeyBuilder {
        private Long id;
        private String name;
        private String keyHash;
        private String salt;
        private String keyPrefix;
        private Long userId;
        private LocalDateTime expirationDate;
        private boolean enabled = true;
        private LocalDateTime lastUsed;
        private long usageCount = 0;
        private Integer rateLimit;
        private Set<String> scopes = new HashSet<>();
        private Set<String> ipRestrictions = new HashSet<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ApiKeyBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApiKeyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ApiKeyBuilder keyHash(String keyHash) {
            this.keyHash = keyHash;
            return this;
        }

        public ApiKeyBuilder salt(String salt) {
            this.salt = salt;
            return this;
        }

        public ApiKeyBuilder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public ApiKeyBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ApiKeyBuilder expirationDate(LocalDateTime expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public ApiKeyBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ApiKeyBuilder lastUsed(LocalDateTime lastUsed) {
            this.lastUsed = lastUsed;
            return this;
        }

        public ApiKeyBuilder usageCount(long usageCount) {
            this.usageCount = usageCount;
            return this;
        }

        public ApiKeyBuilder rateLimit(Integer rateLimit) {
            this.rateLimit = rateLimit;
            return this;
        }

        public ApiKeyBuilder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public ApiKeyBuilder ipRestrictions(Set<String> ipRestrictions) {
            this.ipRestrictions = ipRestrictions;
            return this;
        }

        public ApiKeyBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ApiKeyBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ApiKey build() {
            ApiKey apiKey = new ApiKey();
            apiKey.id = this.id;
            apiKey.name = this.name;
            apiKey.keyHash = this.keyHash;
            apiKey.salt = this.salt;
            apiKey.keyPrefix = this.keyPrefix;
            apiKey.userId = this.userId;
            apiKey.expirationDate = this.expirationDate;
            apiKey.enabled = this.enabled;
            apiKey.lastUsed = this.lastUsed;
            apiKey.usageCount = this.usageCount;
            apiKey.rateLimit = this.rateLimit;
            apiKey.scopes = this.scopes;
            apiKey.ipRestrictions = this.ipRestrictions;
            apiKey.createdAt = this.createdAt;
            apiKey.updatedAt = this.updatedAt;
            return apiKey;
        }
    }
}
