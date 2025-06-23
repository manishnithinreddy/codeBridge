package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.LoadTest;
import com.codebridge.apitest.model.LoadTestStatus;
import com.codebridge.apitest.model.enums.LoadPattern;

import java.time.LocalDateTime;

/**
 * Response DTO for load test operations.
 */
public class LoadTestResponse {
    private Long id;
    private String name;
    private String description;
    private Long testId;
    private Long environmentId;
    private Integer virtualUsers;
    private Integer duration;
    private Integer rampUpTime;
    private Integer rampDownTime;
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
    private Integer requestsPerSecond;
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
        response.setEnvironmentId(loadTest.getEnvironmentId());
        response.setVirtualUsers(loadTest.getVirtualUsers());
        response.setDuration(loadTest.getDuration());
        response.setRampUpTime(loadTest.getRampUpTime());
        response.setRampDownTime(loadTest.getRampDownTime());
        response.setRequestsPerSecond(loadTest.getRequestsPerSecond());
        
        if (loadTest.getLoadPattern() != null) {
            response.setLoadPattern(loadTest.getLoadPattern().name());
        }
        
        if (loadTest.getStatus() != null) {
            response.setStatus(loadTest.getStatus().name());
        }
        
        // Additional fields would be populated from the results JSON
        // This would require parsing the results JSON string
        
        response.setCreatedAt(loadTest.getCreatedAt());
        response.setStartedAt(loadTest.getStartedAt());
        response.setCompletedAt(loadTest.getCompletedAt());
        
        return response;
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

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
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

    public Integer getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(Integer requestsPerSecond) {
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

