package com.codebridge.apitest.model;

import com.codebridge.apitest.model.enums.LoadPattern;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column
    private Long testId;

    @Column
    private Long chainId;

    @Column
    private Long environmentId;

    @Column(nullable = false)
    private Integer virtualUsers;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column
    private Integer rampUpSeconds;

    @Column
    private Integer thinkTimeMs;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoadPattern loadPattern;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoadTestStatus status;

    @Column
    private Integer totalRequests;

    @Column
    private Integer successfulRequests;

    @Column
    private Integer failedRequests;

    @Column
    private Double averageResponseTimeMs;

    @Column
    private Long minResponseTimeMs;

    @Column
    private Long maxResponseTimeMs;

    @Column
    private Long percentile95Ms;

    @Column
    private Long percentile99Ms;

    @Column
    private Double requestsPerSecond;

    @Column
    private Double errorRate;

    @Column
    @Lob
    private String resultSummary;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

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

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
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

    public LoadPattern getLoadPattern() {
        return loadPattern;
    }

    public void setLoadPattern(LoadPattern loadPattern) {
        this.loadPattern = loadPattern;
    }

    public LoadTestStatus getStatus() {
        return status;
    }

    public void setStatus(LoadTestStatus status) {
        this.status = status;
    }

    public Integer getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Integer getSuccessfulRequests() {
        return successfulRequests;
    }

    public void setSuccessfulRequests(Integer successfulRequests) {
        this.successfulRequests = successfulRequests;
    }

    public Integer getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(Integer failedRequests) {
        this.failedRequests = failedRequests;
    }

    public Double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public void setAverageResponseTimeMs(Double averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }

    public Long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    public void setMinResponseTimeMs(Long minResponseTimeMs) {
        this.minResponseTimeMs = minResponseTimeMs;
    }

    public Long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public void setMaxResponseTimeMs(Long maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public Long getPercentile95Ms() {
        return percentile95Ms;
    }

    public void setPercentile95Ms(Long percentile95Ms) {
        this.percentile95Ms = percentile95Ms;
    }

    public Long getPercentile99Ms() {
        return percentile99Ms;
    }

    public void setPercentile99Ms(Long percentile99Ms) {
        this.percentile99Ms = percentile99Ms;
    }

    public Double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(Double requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
}
