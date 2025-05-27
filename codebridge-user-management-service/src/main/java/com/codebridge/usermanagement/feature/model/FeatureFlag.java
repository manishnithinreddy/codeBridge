package com.codebridge.usermanagement.feature.model;

import com.codebridge.core.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing feature flags.
 */
@Entity
@Table(name = "feature_flags")
public class FeatureFlag extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    private String key;

    @Column(nullable = false)
    @NotNull
    private boolean enabled;

    @Column
    private String description;

    @Column
    private UUID teamId;

    @Column
    private boolean isGlobal;

    @Column
    private String rules;

    @Column
    private LocalDateTime expiresAt;

    @Column
    private int rolloutPercentage;

    public FeatureFlag() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(int rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }
}
