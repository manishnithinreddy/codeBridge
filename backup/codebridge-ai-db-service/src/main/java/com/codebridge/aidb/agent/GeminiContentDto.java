package com.codebridge.aidb.agent.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiContentDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<GeminiPartDto> parts;
    private String role; // Optional: "user" or "model"

    public GeminiContentDto() {}

    public GeminiContentDto(List<GeminiPartDto> parts) {
        this.parts = parts;
    }

    public GeminiContentDto(List<GeminiPartDto> parts, String role) {
        this.parts = parts;
        this.role = role;
    }

    public List<GeminiPartDto> getParts() {
        return parts;
    }

    public void setParts(List<GeminiPartDto> parts) {
        this.parts = parts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
