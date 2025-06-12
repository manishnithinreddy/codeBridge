package com.codebridge.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for user activity summary response.
 */
public class UserActivitySummaryResponse {

    private UUID userId;
    private String username;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalActivities;
    private Map<String, Integer> activitiesByType;
    private Map<LocalDate, Integer> activitiesByDate;
    private List<Map<String, Object>> recentActivities;
    private Map<String, Object> serverUsage;
    private Map<String, Object> sessionMetrics;
    private LocalDateTime generatedAt;

    // Default constructor
    public UserActivitySummaryResponse() {
    }

    // Getters and setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(int totalActivities) {
        this.totalActivities = totalActivities;
    }

    public Map<String, Integer> getActivitiesByType() {
        return activitiesByType;
    }

    public void setActivitiesByType(Map<String, Integer> activitiesByType) {
        this.activitiesByType = activitiesByType;
    }

    public Map<LocalDate, Integer> getActivitiesByDate() {
        return activitiesByDate;
    }

    public void setActivitiesByDate(Map<LocalDate, Integer> activitiesByDate) {
        this.activitiesByDate = activitiesByDate;
    }

    public List<Map<String, Object>> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<Map<String, Object>> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public Map<String, Object> getServerUsage() {
        return serverUsage;
    }

    public void setServerUsage(Map<String, Object> serverUsage) {
        this.serverUsage = serverUsage;
    }

    public Map<String, Object> getSessionMetrics() {
        return sessionMetrics;
    }

    public void setSessionMetrics(Map<String, Object> sessionMetrics) {
        this.sessionMetrics = sessionMetrics;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

