package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.HttpMethod;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for test result operations.
 */
public class TestResultResponse {

    private Long id;
    private Long testId;
    private Long environmentId;
    private Long userId;
    private String requestUrl;
    private HttpMethod requestMethod;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private Integer responseStatus;
    private Long responseTime;
    private Boolean passed;
    private LocalDateTime createdAt;
    private Map<String, Object> snapshotComparison;
    private String status;
    private Long executionTimeMs;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public HttpMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(HttpMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public Boolean isPassed() {
        return passed != null && passed;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getSnapshotComparison() {
        return snapshotComparison;
    }

    public void setSnapshotComparison(Map<String, Object> snapshotComparison) {
        this.snapshotComparison = snapshotComparison;
    }
    
    public String getStatus() {
        if (passed != null && passed) {
            return "SUCCESS";
        } else {
            return "FAILED";
        }
    }

    public void setStatus(String status) {
        this.status = status;
        this.passed = "SUCCESS".equals(status);
    }

    public Long getExecutionTimeMs() {
        return responseTime;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        this.responseTime = executionTimeMs;
    }
}

