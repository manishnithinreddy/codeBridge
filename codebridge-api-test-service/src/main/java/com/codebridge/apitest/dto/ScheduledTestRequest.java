package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.enums.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for scheduled test operations.
 */
public class ScheduledTestRequest {

    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private UUID testId;
    
    private UUID chainId;
    
    private UUID loadTestId;
    
    private UUID environmentId;
    
    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;
    
    private String cronExpression;
    
    private Integer fixedRateSeconds;
    
    private LocalDateTime oneTimeExecutionTime;
    
    private String webhookUrl;

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

    public UUID getLoadTestId() {
        return loadTestId;
    }

    public void setLoadTestId(UUID loadTestId) {
        this.loadTestId = loadTestId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Integer getFixedRateSeconds() {
        return fixedRateSeconds;
    }

    public void setFixedRateSeconds(Integer fixedRateSeconds) {
        this.fixedRateSeconds = fixedRateSeconds;
    }

    public LocalDateTime getOneTimeExecutionTime() {
        return oneTimeExecutionTime;
    }

    public void setOneTimeExecutionTime(LocalDateTime oneTimeExecutionTime) {
        this.oneTimeExecutionTime = oneTimeExecutionTime;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}

