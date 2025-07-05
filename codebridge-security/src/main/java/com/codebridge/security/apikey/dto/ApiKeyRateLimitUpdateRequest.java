package com.codebridge.security.apikey.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for API key rate limit update requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyRateLimitUpdateRequest {

    @Min(value = 1, message = "Rate limit must be at least 1")
    private Integer rateLimit;

    // Manual getter for rateLimit field
    public Integer getRateLimit() {
        return rateLimit;
    }
}
