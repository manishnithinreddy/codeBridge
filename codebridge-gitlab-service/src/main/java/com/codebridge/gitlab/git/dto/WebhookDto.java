package com.codebridge.gitlab.git.dto;

import com.codebridge.gitlab.git.model.Webhook.WebhookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDto {
    
    private UUID id;
    
    @NotBlank(message = "Webhook name is required")
    private String name;
    
    private String description;
    
    private String remoteId;
    
    @NotBlank(message = "Payload URL is required")
    private String payloadUrl;
    
    private String secretToken;
    
    @NotBlank(message = "Content type is required")
    private String contentType;
    
    @NotBlank(message = "Events are required")
    private String events;
    
    @NotNull(message = "Status is required")
    private WebhookStatus status;
    
    private LocalDateTime lastTriggeredAt;
    
    private Integer lastResponseCode;
    
    private String lastResponseMessage;
    
    private Integer failureCount;
    
    @NotNull(message = "Repository ID is required")
    private UUID repositoryId;
    
    private String repositoryName;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

