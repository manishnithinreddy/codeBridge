package com.codebridge.apitest.model;

import com.codebridge.apitest.model.enums.ScheduleType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for scheduled tests.
 */
@Entity
@Table(name = "scheduled_tests")
public class ScheduledTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    @Column
    private String cronExpression;

    @Column
    private Integer intervalMinutes;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "chain_id")
    private Long chainId;

    @Column(name = "load_test_id")
    private Long loadTestId;

    @Column(name = "collection_id")
    private Long collectionId;

    @Column(name = "environment_id")
    private Long environmentId;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "fixed_rate_seconds")
    private Integer fixedRateSeconds;

    @Column(name = "one_time_execution_time")
    private LocalDateTime oneTimeExecutionTime;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;
    
    @Column(name = "execution_count")
    private Integer executionCount = 0;

    @Column(name = "last_execution_start_time")
    private LocalDateTime lastExecutionStartTime;

    @Column(name = "last_execution_end_time")
    private LocalDateTime lastExecutionEndTime;

    @Column(name = "last_execution_success")
    private Boolean lastExecutionSuccess;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScheduledTestStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (enabled == null) {
            enabled = true;
        }
        if (executionCount == null) {
            executionCount = 0;
        }
        if (status == null) {
            status = ScheduledTestStatus.IDLE;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isActive() {
        return enabled != null && enabled;
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

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
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

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }
    
    public Integer getExecutionCount() {
        return executionCount != null ? executionCount : 0;
    }
    
    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }
    
    public void incrementExecutionCount() {
        if (this.executionCount == null) {
            this.executionCount = 1;
        } else {
            this.executionCount++;
        }
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

    public ScheduledTestStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduledTestStatus status) {
        this.status = status;
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

