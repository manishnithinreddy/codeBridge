package com.codebridge.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
}

