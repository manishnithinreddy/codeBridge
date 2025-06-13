package com.codebridge.monitoring.platform.ops.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDto {
    private Long id;
    private String name;
    private String url;
    private String description;
    private Long organizationId;
    private List<String> eventTypes;
    private boolean active;
    private String secretKey;
    private Map<String, String> headers;
    private int retryCount;
    private int timeoutSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastTriggeredAt;
}

