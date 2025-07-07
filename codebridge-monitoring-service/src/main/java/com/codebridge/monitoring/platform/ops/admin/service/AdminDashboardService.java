package com.codebridge.monitoring.platform.ops.admin.service;

import com.codebridge.monitoring.platform.ops.admin.dto.DashboardStatsDto;
import com.codebridge.monitoring.platform.ops.admin.dto.SystemHealthDto;

/**
 * Service interface for admin dashboard operations.
 * Provides methods for system monitoring and management.
 */
public interface AdminDashboardService {

    /**
     * Get system statistics for the admin dashboard.
     *
     * @return Dashboard statistics
     */
    DashboardStatsDto getDashboardStats();

    /**
     * Get system health information.
     *
     * @return System health data
     */
    SystemHealthDto getSystemHealth();

    /**
     * Trigger a system maintenance task.
     *
     * @param taskName Name of the maintenance task
     */
    void triggerMaintenanceTask(String taskName);
}
