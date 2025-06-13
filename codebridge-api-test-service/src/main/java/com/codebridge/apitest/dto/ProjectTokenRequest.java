package com.codebridge.apitest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request DTO for project token operations.
 */
public class ProjectTokenRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Token type cannot be blank")
    @Pattern(regexp = "Bearer|Basic|ApiKey|OAuth2|Custom", message = "Token type must be one of: Bearer, Basic, ApiKey, OAuth2, Custom")
    private String tokenType;

    @NotBlank(message = "Token value cannot be blank")
    private String tokenValue;

    @Size(max = 255, message = "Header name cannot exceed 255 characters")
    private String headerName;

    @Size(max = 255, message = "Parameter name cannot exceed 255 characters")
    private String parameterName;

    @Pattern(regexp = "header|query|cookie|body", message = "Token location must be one of: header, query, cookie, body")
    private String tokenLocation;

    private LocalDateTime expiresAt;

    private String refreshUrl;

    private String refreshData;

    @NotNull(message = "Active status cannot be null")
    private Boolean active;

    private Boolean autoRefresh;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getTokenLocation() {
        return tokenLocation;
    }

    public void setTokenLocation(String tokenLocation) {
        this.tokenLocation = tokenLocation;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public void setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    public String getRefreshData() {
        return refreshData;
    }

    public void setRefreshData(String refreshData) {
        this.refreshData = refreshData;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(Boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }
}

