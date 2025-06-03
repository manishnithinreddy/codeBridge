package com.codebridge.aidb.dto.ai;

import java.io.Serializable;

public class AiTextToSqlRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String prompt;

    public AiTextToSqlRequestDto() {
    }

    public AiTextToSqlRequestDto(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
