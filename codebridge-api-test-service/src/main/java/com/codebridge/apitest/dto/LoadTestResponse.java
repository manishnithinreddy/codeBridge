package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.LoadTest;
import com.codebridge.apitest.model.LoadTestStatus;
import com.codebridge.apitest.model.enums.LoadPattern;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for load test operations.
 */
public class LoadTestResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID testId;
    private UUID chainId;
    private UUID environmentId;
    private Integer virtualUsers;
    private Integer durationSeconds;
    private Integer rampUpSeconds;
    private Integer thinkTimeMs;
    private String loadPattern;
    private String status;
    private Integer totalRequests;
    private Integer successfulRequests;
    private Integer failedRequests;
    private Double averageResponseTimeMs;
    private Long minResponseTimeMs;
    private Long maxResponseTimeMs;
    private Long percentile95Ms;
    private Long percentile99Ms;
    private Double requestsPerSecond;
    private Double errorRate;
    private String resultSummary;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * Converts a LoadTest entity to a LoadTestResponse DTO.
     *
     * @param loadTest the load test entity
     * @return the load test response DTO
     */
    public static LoadTestResponse fromEntity(LoadTest loadTest) {
        LoadTestResponse response = new LoadTestResponse();
        response.setId(loadTest.getId());
        response.setName(loadTest.getName());
        response.setDescription(loadTest.getDescription());
        response.setTestId(loadTest.getTestId());
        response.setChainId(loadTest.getChainId());
        response.setEnvironmentId(loadTest.getEnvironmentId());
        response.setVirtualUsers(loadTest.getVirtualUsers());
        response.setDurationSeconds(loadTest.getDurationSeconds());
        response.setRampUpSeconds(loadTest.getRampUpSeconds());
        response.setThinkTimeMs(loadTest.getThinkTimeMs());
        
        if (loadTest.getLoadPattern() != null) {
            response.setLoadPattern(loadTest.getLoadPattern().name());
        }
        
        if (loadTest.getStatus() != null) {
            response.setStatus(loadTest.getStatus().name());
        }
        
        response.setTotalRequests(loadTest.getTotalRequests());
        response.setSuccessfulRequests(loadTest.getSuccessfulRequests());
        response.setFailedRequests(loadTest.getFailedRequests());
        response.setAverageResponseTimeMs(loadTest.getAverageResponseTimeMs());
        response.setMinResponseTimeMs(loadTest.getMinResponseTimeMs());
        response.setMaxResponseTimeMs(loadTest.getMaxResponseTimeMs());
        response.setPercentile95Ms(loadTest.getPercentile95Ms());
        response.setPercentile99Ms(loadTest.getPercentile99Ms());
        response.setRequestsPerSecond(loadTest.getRequestsPerSecond());
        response.setErrorRate(loadTest.getErrorRate());
        response.setResultSummary(loadTest.getResultSummary());
        response.setCreatedAt(loadTest.getCreatedAt());
        response.setStartedAt(loadTest.getStartedAt());
        response.setCompletedAt(loadTest.getCompletedAt());
        
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

