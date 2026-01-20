package com.codebridge.aidb.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
// import java.util.List; // For SafetyRatingDto if used

// Placeholder for SafetyRatingDto if needed later
// class SafetyRatingDto implements Serializable { /* ... */ }

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiCandidateDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private GeminiContentDto content;
    private String finishReason;
    // private List<SafetyRatingDto> safetyRatings;

    public GeminiCandidateDto() {}

    public GeminiContentDto getContent() {
        return content;
    }

    public void setContent(GeminiContentDto content) {
        this.content = content;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    // Add getters/setters for safetyRatings if included
}
