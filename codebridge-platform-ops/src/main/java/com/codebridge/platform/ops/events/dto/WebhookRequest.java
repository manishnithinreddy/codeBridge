package com.codebridge.platform.ops.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class WebhookRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @NotBlank
    @Size(max = 255)
    private String url;
    
    @Size(max = 255)
    private String description;
    
    @NotNull
    private Long organizationId;
    
    @NotEmpty
    private List<String> eventTypes;
    
    private boolean active = true;
    
    private String secretKey;
    
    private Map<String, String> headers;
    
    private int retryCount = 3;
    
    private int timeoutSeconds = 30;
}

