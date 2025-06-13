package com.codebridge.platform.ops.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalUsers;
    private long totalOrganizations;
    private long totalTeams;
    private long activeUsers;
    private long newUsersToday;
    private Map<String, Long> usersByRole;
    private Map<String, Long> serviceUsage;
    private Map<String, Double> systemResources;
}

