package com.codebridge.monitoring.platform.ops.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDto {
    private String status;
    private Map<String, ServiceHealthDto> services;
    private Map<String, String> diskSpace;
    private Map<String, String> memory;
    private List<String> warnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceHealthDto {
        private String status;
        private String version;
        private String uptime;
        private Map<String, String> details;
    }
}

