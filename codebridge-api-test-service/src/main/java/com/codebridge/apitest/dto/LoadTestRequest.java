package com.codebridge.apitest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for load test operations.
 */
public class LoadTestRequest {

    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private UUID testId;
    
    private UUID chainId;
    
    private UUID environmentId;
    
    @NotNull(message = "Virtual users count is required")
    @Min(value = 1, message = "Virtual users must be at least 1")
    private Integer virtualUsers;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;
    
    @Min(value = 0, message = "Ramp-up time cannot be negative")
    private Integer rampUpSeconds;
    
    @Min(value = 0, message = "Think time cannot be negative")
    private Integer thinkTimeMs;
    
    private String loadPattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getTestId() {
        return testId;
    }

    public void setTestId(UUID testId) {
        this.testId = testId;
    }

    public UUID getChainId() {
        return chainId;
    }

    public void setChainId(UUID chainId) {
        this.chainId = chainId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public Integer getVirtualUsers() {
        return virtualUsers;
    }

    public void setVirtualUsers(Integer virtualUsers) {
        this.virtualUsers = virtualUsers;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getRampUpSeconds() {
        return rampUpSeconds;
    }

    public void setRampUpSeconds(Integer rampUpSeconds) {
        this.rampUpSeconds = rampUpSeconds;
    }

    public Integer getThinkTimeMs() {
        return thinkTimeMs;
    }

    public void setThinkTimeMs(Integer thinkTimeMs) {
        this.thinkTimeMs = thinkTimeMs;
    }

    public String getLoadPattern() {
        return loadPattern;
    }

    public void setLoadPattern(String loadPattern) {
        this.loadPattern = loadPattern;
    }
}

