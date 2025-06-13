package com.codebridge.apitest.model;

import com.codebridge.apitest.model.enums.ScheduleType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for scheduled tests.
 */
@Entity
@Table(name = "scheduled_tests")
public class ScheduledTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private UUID userId;

    @Column
    private UUID testId;

    @Column
    private UUID chainId;

    @Column
    private UUID loadTestId;

    @Column
    private UUID environmentId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    @Column
    private String cronExpression;

    @Column
    private Integer fixedRateSeconds;

    @Column
    private LocalDateTime oneTimeExecutionTime;

    @Column
    private String webhookUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScheduledTestStatus status;

    @Column
    private LocalDateTime lastExecutionStartTime;

    @Column
    private LocalDateTime lastExecutionEndTime;

    @Column
    private Boolean lastExecutionSuccess;

    @Column
    @Lob
    private String lastErrorMessage;

    @Column
    private Integer executionCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Getters and Setters
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ScheduledTestStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduledTestStatus status) {
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

