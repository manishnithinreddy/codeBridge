package com.codebridge.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for MFA verification requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MfaVerificationRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "MFA token is required")
    private String mfaToken;

    @NotBlank(message = "MFA code is required")
    private String mfaCode;
}

