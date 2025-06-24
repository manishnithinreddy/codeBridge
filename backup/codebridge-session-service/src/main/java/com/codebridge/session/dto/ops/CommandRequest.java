package com.codebridge.session.dto.ops;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.util.UUID;

public class CommandRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Command cannot be blank")
    private String command;

    @Min(value = 100, message = "Timeout must be at least 100 milliseconds")
    private Integer timeout; // Milliseconds
    
    private String commandId;

    // Default constructor
    public CommandRequest() {
        // Generate a unique command ID by default
        this.commandId = UUID.randomUUID().toString();
    }

    // Constructor with command
    public CommandRequest(String command) {
        this();
        this.command = command;
    }

    // Constructor with command and timeout
    public CommandRequest(String command, Integer timeout) {
        this(command);
        this.timeout = timeout;
    }

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
    
    public String getCommandId() {
        return commandId;
    }
    
    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }
    
    // Alias for getTimeout for backward compatibility
    public Integer getTimeoutMs() {
        return timeout;
    }
}

