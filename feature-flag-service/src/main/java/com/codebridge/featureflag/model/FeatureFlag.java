package com.codebridge.featureflag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a feature flag with its configuration and rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag implements Serializable {
    
    private String key;
    private String namespace;
    private FlagValue value;
    private String description;
    private Map<String, String> tags;
    private boolean temporary;
    private Instant expirationTime;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private String version;
    private List<FlagRule> rules;
    private Map<String, Double> distribution;
    
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Checks if the flag has expired.
     * 
     * @return true if the flag is temporary and has expired, false otherwise
     */
    public boolean isExpired() {
        return temporary && expirationTime != null && Instant.now().isAfter(expirationTime);
    }
}

