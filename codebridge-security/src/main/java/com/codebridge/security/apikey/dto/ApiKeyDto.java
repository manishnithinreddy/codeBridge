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
}

