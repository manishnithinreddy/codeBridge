package com.codebridge.session.dto;

import java.util.List;

/**
 * Response DTO for host key list operations
 */
public class HostKeyListResponse {
    
    private List<HostKeyDTO> hostKeys;
    private String currentPolicy;
    
    // Default constructor for deserialization
    public HostKeyListResponse() {
    }
    
    // Constructor with all fields
    public HostKeyListResponse(List<HostKeyDTO> hostKeys, String currentPolicy) {
        this.hostKeys = hostKeys;
        this.currentPolicy = currentPolicy;
    }
    
    // Getters and setters
    public List<HostKeyDTO> getHostKeys() {
        return hostKeys;
    }
    
    public void setHostKeys(List<HostKeyDTO> hostKeys) {
        this.hostKeys = hostKeys;
    }
    
    public String getCurrentPolicy() {
        return currentPolicy;
    }
    
    public void setCurrentPolicy(String currentPolicy) {
        this.currentPolicy = currentPolicy;
    }
}

