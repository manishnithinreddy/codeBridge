package com.codebridge.security.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for API key creation requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyCreationRequest {

    @NotBlank(message = "API key name is required")
    private String name;

    private Integer expirationDays;

    private Set<String> scopes = new HashSet<>();

    private Set<String> ipRestrictions = new HashSet<>();

    private Integer rateLimit;

    // Manual getter for rateLimit field
    public Integer getRateLimit() {
        return rateLimit;
    }

    // Manual getter for scopes field
    public Set<String> getScopes() {
        return scopes;
    }

    // Manual getter for ipRestrictions field
    public Set<String> getIpRestrictions() {
        return ipRestrictions;
    }

    // Manual getter for name field
    public String getName() {
        return name;
    }

    // Manual getter for expirationDays field
    public Integer getExpirationDays() {
        return expirationDays;
    }
}
