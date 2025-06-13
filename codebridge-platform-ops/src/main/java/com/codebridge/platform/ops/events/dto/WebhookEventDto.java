package com.codebridge.platform.ops.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventDto {
    private Long id;
    private Long webhookId;
    private String eventType;
    private String status;
    private Map<String, Object> payload;
    private String responseBody;
    private int responseCode;
    private int retryCount;
    private int maxRetries;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}

