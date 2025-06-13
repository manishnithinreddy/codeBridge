package com.codebridge.apitest.dto;

/**
 * DTO for load test results.
 */
public class LoadTestResult {
    private final int totalRequests;
    private final int successfulRequests;
    private final int failedRequests;
    private final double averageResponseTimeMs;
    private final long minResponseTimeMs;
    private final long maxResponseTimeMs;
    private final long percentile95Ms;
    private final long percentile99Ms;
    private final double requestsPerSecond;
    private final double errorRate;
    private final String summary;

    public LoadTestResult(int totalRequests, int successfulRequests, int failedRequests,
                         double averageResponseTimeMs, long minResponseTimeMs, long maxResponseTimeMs,
                         long percentile95Ms, long percentile99Ms, double requestsPerSecond,
                         double errorRate, String summary) {
        this.totalRequests = totalRequests;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.minResponseTimeMs = minResponseTimeMs;
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.percentile95Ms = percentile95Ms;
        this.percentile99Ms = percentile99Ms;
        this.requestsPerSecond = requestsPerSecond;
        this.errorRate = errorRate;
        this.summary = summary;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getSuccessfulRequests() {
        return successfulRequests;
    }

    public int getFailedRequests() {
        return failedRequests;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public long getPercentile95Ms() {
        return percentile95Ms;
    }

    public long getPercentile99Ms() {
        return percentile99Ms;
    }

    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public String getSummary() {
        return summary;
    }
}

