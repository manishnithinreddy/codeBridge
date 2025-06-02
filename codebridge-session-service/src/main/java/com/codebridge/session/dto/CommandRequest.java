package com.codebridge.session.dto; // Adapted package

import jakarta.validation.constraints.NotBlank;

public class CommandRequest {

    @NotBlank(message = "Command cannot be blank")
    private String command;

    private Integer timeout; // in seconds

    // Getters and Setters
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}
