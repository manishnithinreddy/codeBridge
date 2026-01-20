package com.codebridge.aidb.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

// Placeholder for PromptFeedbackDto if needed later
// class PromptFeedbackDto implements Serializable { /* ... */ }

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiTextToSqlResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<GeminiCandidateDto> candidates;
    private String error; // For high-level errors (e.g., API key issue, quota)
    // private PromptFeedbackDto promptFeedback;

    public AiTextToSqlResponseDto() {}

    public List<GeminiCandidateDto> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GeminiCandidateDto> candidates) {
        this.candidates = candidates;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Add getter/setter for promptFeedback if included
}
