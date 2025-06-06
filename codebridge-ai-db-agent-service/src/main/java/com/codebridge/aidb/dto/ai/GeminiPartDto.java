package com.codebridge.aidb.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiPartDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String text;
    // In future, could add 'inlineData' for multimodal features

    public GeminiPartDto() {}

    public GeminiPartDto(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
