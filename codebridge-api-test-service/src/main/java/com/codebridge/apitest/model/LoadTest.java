package com.codebridge.apitest.model;

import com.codebridge.apitest.model.enums.LoadPattern;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for load tests.
 */
@Entity
@Table(name = "load_tests")
public class LoadTest {

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
    private Long testId;

    @Column(nullable = false)
    private Integer duration; // in seconds

    @Column(nullable = false)
    private Integer virtualUsers;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoadPattern loadPattern;

    @Column
    private Integer rampUpTime; // in seconds

    @Column
    private Integer rampDownTime; // in seconds

    @Column
    private Integer requestsPerSecond;

    @Column
    private Long environmentId;

    @Column
    @Lob
    private String results; // JSON string with load test results

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoadTestStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getVirtualUsers() {
        return virtualUsers;
    }

    public void setVirtualUsers(Integer virtualUsers) {
        this.virtualUsers = virtualUsers;
    }

    public LoadPattern getLoadPattern() {
        return loadPattern;
    }

    public void setLoadPattern(LoadPattern loadPattern) {
        this.loadPattern = loadPattern;
    }

    public Integer getRampUpTime() {
        return rampUpTime;
    }

    public void setRampUpTime(Integer rampUpTime) {
        this.rampUpTime = rampUpTime;
    }

    public Integer getRampDownTime() {
        return rampDownTime;
    }

    public void setRampDownTime(Integer rampDownTime) {
        this.rampDownTime = rampDownTime;
    }

    public Integer getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(Integer requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LoadTestStatus getStatus() {
        return status;
    }

    public void setStatus(LoadTestStatus status) {
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
    
    // Additional methods for compatibility
    
    public Integer getThinkTimeMs() {
        return 0; // Default value
    }
    
    public void setThinkTimeMs(Integer thinkTimeMs) {
        // No-op for compatibility
    }
    
    public Integer getDurationSeconds() {
        return duration;
    }
    
    public void setDurationSeconds(Integer durationSeconds) {
        this.duration = durationSeconds;
    }
    
    public Integer getRampUpSeconds() {
        return rampUpTime;
    }
    
    public void setRampUpSeconds(Integer rampUpSeconds) {
        this.rampUpTime = rampUpSeconds;
    }
}

