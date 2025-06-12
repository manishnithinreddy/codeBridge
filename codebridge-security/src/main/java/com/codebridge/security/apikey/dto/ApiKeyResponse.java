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
}

