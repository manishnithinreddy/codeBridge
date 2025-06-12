package com.codebridge.admin.controller;

import com.codebridge.admin.dto.DashboardSummaryResponse;
import com.codebridge.admin.dto.SystemHealthResponse;
import com.codebridge.admin.dto.UserActivitySummaryResponse;
import com.codebridge.admin.service.AdminDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for admin dashboard operations.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    
    private final AdminDashboardService dashboardService;
    
    @Autowired
    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    /**
     * Get dashboard summary data.
     *
     * @return dashboard summary response
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        logger.info("Getting dashboard summary");
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
    
    /**
     * Get system health information.
     *
     * @return system health response
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        logger.info("Getting system health information");
        return ResponseEntity.ok(dashboardService.getSystemHealth());
    }
    
    /**
     * Get user activity summary.
     *
     * @param userId optional user ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return user activity summary response
     */
    @GetMapping("/user-activity")
    public ResponseEntity<UserActivitySummaryResponse> getUserActivitySummary(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        logger.info("Getting user activity summary for user: {}, startDate: {}, endDate: {}", 
                userId, startDate, endDate);
        
        return ResponseEntity.ok(dashboardService.getUserActivitySummary(userId, startDate, endDate));
    }
}

