package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.enums.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for scheduled test operations.
 */
public class ScheduledTestRequest {

    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private Long testId;
    
    private Long chainId;
    
    private Long loadTestId;
    
    private Long environmentId;
    
    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;
    
    private String cronExpression;
    
    private Integer intervalMinutes;

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

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public Long getLoadTestId() {
        return loadTestId;
    }

    public void setLoadTestId(Long loadTestId) {
        this.loadTestId = loadTestId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
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

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }
}

