package com.codebridge.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for dashboard summary response.
 */
public class DashboardSummaryResponse {

    private int totalUsers;
    private int activeUsers;
    private int totalServers;
    private int activeServers;
    private int totalSessions;
    private int activeSessions;
    private Map<String, Integer> usersByRole;
    private Map<String, Integer> serversByStatus;
    private Map<String, Integer> sessionsByType;
    private List<Map<String, Object>> recentActivity;
    private Map<String, Integer> activityByType;
    private LocalDateTime generatedAt;

    // Default constructor
    public DashboardSummaryResponse() {
    }

    // Getters and setters
    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getTotalServers() {
        return totalServers;
    }

    public void setTotalServers(int totalServers) {
        this.totalServers = totalServers;
    }

    public int getActiveServers() {
        return activeServers;
    }

    public void setActiveServers(int activeServers) {
        this.activeServers = activeServers;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public Map<String, Integer> getUsersByRole() {
        return usersByRole;
    }

    public void setUsersByRole(Map<String, Integer> usersByRole) {
        this.usersByRole = usersByRole;
    }

    public Map<String, Integer> getServersByStatus() {
        return serversByStatus;
    }

    public void setServersByStatus(Map<String, Integer> serversByStatus) {
        this.serversByStatus = serversByStatus;
    }

    public Map<String, Integer> getSessionsByType() {
        return sessionsByType;
    }

    public void setSessionsByType(Map<String, Integer> sessionsByType) {
        this.sessionsByType = sessionsByType;
    }

    public List<Map<String, Object>> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<Map<String, Object>> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public Map<String, Integer> getActivityByType() {
        return activityByType;
    }

    public void setActivityByType(Map<String, Integer> activityByType) {
        this.activityByType = activityByType;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

