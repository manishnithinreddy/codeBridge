package com.codebridge.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for system health response.
 */
public class SystemHealthResponse {

    private String overallStatus;
    private Map<String, String> serviceStatuses;
    private Map<String, Double> systemMetrics;
    private Map<String, Object> databaseMetrics;
    private List<Map<String, Object>> recentErrors;
    private Map<String, Object> resourceUtilization;
    private LocalDateTime generatedAt;

    // Default constructor
    public SystemHealthResponse() {
    }

    // Getters and setters
    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Map<String, String> getServiceStatuses() {
        return serviceStatuses;
    }

    public void setServiceStatuses(Map<String, String> serviceStatuses) {
        this.serviceStatuses = serviceStatuses;
    }

    public Map<String, Double> getSystemMetrics() {
        return systemMetrics;
    }

    public void setSystemMetrics(Map<String, Double> systemMetrics) {
        this.systemMetrics = systemMetrics;
    }

    public Map<String, Object> getDatabaseMetrics() {
        return databaseMetrics;
    }

    public void setDatabaseMetrics(Map<String, Object> databaseMetrics) {
        this.databaseMetrics = databaseMetrics;
    }

    public List<Map<String, Object>> getRecentErrors() {
        return recentErrors;
    }

    public void setRecentErrors(List<Map<String, Object>> recentErrors) {
        this.recentErrors = recentErrors;
    }

    public Map<String, Object> getResourceUtilization() {
        return resourceUtilization;
    }

    public void setResourceUtilization(Map<String, Object> resourceUtilization) {
        this.resourceUtilization = resourceUtilization;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

