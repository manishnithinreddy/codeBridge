package com.codebridge.aidb.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

// Placeholder for GenerationConfig and SafetySetting if needed later
// class GenerationConfigDto implements Serializable { /* ... */ }
// class SafetySettingDto implements Serializable { /* ... */ }

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiRequestPayloadDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<GeminiContentDto> contents;
    // private GenerationConfigDto generationConfig;
    // private List<SafetySettingDto> safetySettings;

    public GeminiRequestPayloadDto() {}

    public GeminiRequestPayloadDto(List<GeminiContentDto> contents) {
        this.contents = contents;
    }

    public List<GeminiContentDto> getContents() {
        return contents;
    }

    public void setContents(List<GeminiContentDto> contents) {
        this.contents = contents;
    }

    // Add getters/setters for generationConfig and safetySettings if they are included
}
