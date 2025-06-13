package com.codebridge.monitoring.platform.ops.admin.controller;

import com.codebridge.monitoring.platform.ops.admin.dto.DashboardStatsDto;
import com.codebridge.monitoring.platform.ops.admin.dto.SystemHealthDto;
import com.codebridge.monitoring.platform.ops.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for admin dashboard operations.
 * Provides endpoints for system monitoring and management.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    /**
     * Get system statistics for the admin dashboard.
     *
     * @return Dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    /**
     * Get system health information.
     *
     * @return System health data
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        return ResponseEntity.ok(dashboardService.getSystemHealth());
    }

    /**
     * Trigger a system maintenance task.
     *
     * @param taskName Name of the maintenance task
     * @return Success response
     */
    @PostMapping("/maintenance/{taskName}")
    public ResponseEntity<?> triggerMaintenanceTask(@PathVariable String taskName) {
        dashboardService.triggerMaintenanceTask(taskName);
        return ResponseEntity.ok().build();
    }
}

