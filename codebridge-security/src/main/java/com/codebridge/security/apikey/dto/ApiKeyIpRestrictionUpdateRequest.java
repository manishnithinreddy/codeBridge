package com.codebridge.security.apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for API key IP restriction update requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyIpRestrictionUpdateRequest {

    private Set<String> ipRestrictions = new HashSet<>();

    // Manual getter for ipRestrictions field
    public Set<String> getIpRestrictions() {
        return ipRestrictions;
    }
}
