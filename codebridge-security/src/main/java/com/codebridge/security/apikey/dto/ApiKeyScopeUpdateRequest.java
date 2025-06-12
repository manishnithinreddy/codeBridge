package com.codebridge.security.apikey.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for API key scope update requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyScopeUpdateRequest {

    @NotEmpty(message = "Scopes cannot be empty")
    private Set<String> scopes = new HashSet<>();
}

