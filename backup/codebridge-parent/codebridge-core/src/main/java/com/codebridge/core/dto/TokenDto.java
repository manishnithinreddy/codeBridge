package com.codebridge.core.dto;

import com.codebridge.core.model.Token.TokenType;
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
public class TokenDto {
    
    private UUID id;
    
    @NotBlank(message = "Token value is required")
    private String token;
    
    @NotNull(message = "Token type is required")
    private TokenType tokenType;
    
    @NotNull(message = "Expiration date is required")
    private LocalDateTime expiresAt;
    
    private boolean revoked;
    
    private LocalDateTime lastUsedAt;
    
    private String lastUsedIp;
    
    private String userAgent;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

