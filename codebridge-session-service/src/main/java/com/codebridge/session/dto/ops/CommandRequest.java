package com.codebridge.session.dto.ops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.io.Serializable;

public class CommandRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Command cannot be blank")
    private String command;

    @Min(value = 100, message = "Timeout must be at least 100 milliseconds")
    private Integer timeout; // Milliseconds

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
