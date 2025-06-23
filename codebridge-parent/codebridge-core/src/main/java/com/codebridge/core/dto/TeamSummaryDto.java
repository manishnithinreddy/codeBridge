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
public class TeamSummaryDto {
    
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean isDefault;
    private boolean isPersonal;
    private String ownerUserId;
}

