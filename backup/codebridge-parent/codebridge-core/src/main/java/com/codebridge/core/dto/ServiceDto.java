package com.codebridge.core.dto;

import com.codebridge.core.model.Service.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ServiceDto {
    
    private UUID id;
    
    @NotBlank(message = "Service name is required")
    @Size(min = 3, max = 50, message = "Service name must be between 3 and 50 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;
    
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;
    
    private String baseUrl;
    
    private String apiUrl;
    
    private String iconUrl;
    
    private boolean isEnabled;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

