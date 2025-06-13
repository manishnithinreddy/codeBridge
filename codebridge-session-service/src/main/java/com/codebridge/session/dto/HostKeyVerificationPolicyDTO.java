package com.codebridge.session.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for host key verification policy
 */
public class HostKeyVerificationPolicyDTO {
    
    @NotBlank(message = "Policy is required")
    private String policy;
    
    // Default constructor for deserialization
    public HostKeyVerificationPolicyDTO() {
    }
    
    // Constructor with policy
    public HostKeyVerificationPolicyDTO(String policy) {
        this.policy = policy;
    }
    
    // Getters and setters
    public String getPolicy() {
        return policy;
    }
    
    public void setPolicy(String policy) {
        this.policy = policy;
    }
}

