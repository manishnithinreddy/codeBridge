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
}

