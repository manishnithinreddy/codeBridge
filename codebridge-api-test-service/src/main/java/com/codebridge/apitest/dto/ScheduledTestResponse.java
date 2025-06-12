package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.ScheduledTest;
import com.codebridge.apitest.model.ScheduledTestStatus;
import com.codebridge.apitest.model.enums.ScheduleType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for scheduled test operations.
 */
public class ScheduledTestResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID testId;
    private UUID chainId;
    private UUID loadTestId;
    private UUID environmentId;
    private String scheduleType;
    private String cronExpression;
    private Integer fixedRateSeconds;
    private LocalDateTime oneTimeExecutionTime;
    private String webhookUrl;
    private boolean active;
    private String status;
    private LocalDateTime lastExecutionStartTime;
    private LocalDateTime lastExecutionEndTime;
    private Boolean lastExecutionSuccess;
    private String lastErrorMessage;
    private Integer executionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts a ScheduledTest entity to a ScheduledTestResponse DTO.
     *
     * @param scheduledTest the scheduled test entity
     * @return the scheduled test response DTO
     */
    public static ScheduledTestResponse fromEntity(ScheduledTest scheduledTest) {
        ScheduledTestResponse response = new ScheduledTestResponse();
        response.setId(scheduledTest.getId());
        response.setName(scheduledTest.getName());
        response.setDescription(scheduledTest.getDescription());
        response.setTestId(scheduledTest.getTestId());
        response.setChainId(scheduledTest.getChainId());
        response.setLoadTestId(scheduledTest.getLoadTestId());
        response.setEnvironmentId(scheduledTest.getEnvironmentId());
        
        if (scheduledTest.getScheduleType() != null) {
            response.setScheduleType(scheduledTest.getScheduleType().name());
        }
        
        response.setCronExpression(scheduledTest.getCronExpression());
        response.setFixedRateSeconds(scheduledTest.getFixedRateSeconds());
        response.setOneTimeExecutionTime(scheduledTest.getOneTimeExecutionTime());
        response.setWebhookUrl(scheduledTest.getWebhookUrl());
        response.setActive(scheduledTest.isActive());
        
        if (scheduledTest.getStatus() != null) {
            response.setStatus(scheduledTest.getStatus().name());
        }
        
        response.setLastExecutionStartTime(scheduledTest.getLastExecutionStartTime());
        response.setLastExecutionEndTime(scheduledTest.getLastExecutionEndTime());
        response.setLastExecutionSuccess(scheduledTest.getLastExecutionSuccess());
        response.setLastErrorMessage(scheduledTest.getLastErrorMessage());
        response.setExecutionCount(scheduledTest.getExecutionCount());
        response.setCreatedAt(scheduledTest.getCreatedAt());
        response.setUpdatedAt(scheduledTest.getUpdatedAt());
        
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastExecutionStartTime() {
        return lastExecutionStartTime;
    }

    public void setLastExecutionStartTime(LocalDateTime lastExecutionStartTime) {
        this.lastExecutionStartTime = lastExecutionStartTime;
    }

    public LocalDateTime getLastExecutionEndTime() {
        return lastExecutionEndTime;
    }

    public void setLastExecutionEndTime(LocalDateTime lastExecutionEndTime) {
        this.lastExecutionEndTime = lastExecutionEndTime;
    }

    public Boolean getLastExecutionSuccess() {
        return lastExecutionSuccess;
    }

    public void setLastExecutionSuccess(Boolean lastExecutionSuccess) {
        this.lastExecutionSuccess = lastExecutionSuccess;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

