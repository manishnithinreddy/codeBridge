package com.codebridge.security.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @Size(max = 255)
    private String description;
    
    private String logoUrl;
    
    private String websiteUrl;

    // Manual getter for name field
    public String getName() {
        return name;
    }

    // Manual getter for description field
    public String getDescription() {
        return description;
    }

    // Manual getter for logoUrl field
    public String getLogoUrl() {
        return logoUrl;
    }

    // Manual getter for websiteUrl field
    public String getWebsiteUrl() {
        return websiteUrl;
    }
}
