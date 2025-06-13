package com.codebridge.session.dto.ops;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for streaming command output messages.
 */
public class CommandOutputMessage {

    /**
     * Type of command output.
     */
    public enum OutputType {
        STDOUT,
        STDERR,
        EXIT_CODE,
        START,
        END,
        ERROR
    }

    private UUID sessionId;
    private UUID commandId;
    private OutputType type;
    private String content;
    private Instant timestamp;
    private boolean complete;
    private Integer exitCode;

    // Default constructor
    public CommandOutputMessage() {
    }

    // Constructor for standard output/error messages
    public CommandOutputMessage(UUID sessionId, UUID commandId, OutputType type, String content) {
        this.sessionId = sessionId;
        this.commandId = commandId;
        this.type = type;
        this.content = content;
        this.timestamp = Instant.now();
        this.complete = false;
        this.exitCode = null;
    }

    // Constructor for completion messages with exit code
    public CommandOutputMessage(UUID sessionId, UUID commandId, int exitCode) {
        this.sessionId = sessionId;
        this.commandId = commandId;
        this.type = OutputType.EXIT_CODE;
        this.content = "Command completed with exit code: " + exitCode;
        this.timestamp = Instant.now();
        this.complete = true;
        this.exitCode = exitCode;
    }

    // Constructor for start messages
    public static CommandOutputMessage createStartMessage(UUID sessionId, UUID commandId, String command) {
        CommandOutputMessage message = new CommandOutputMessage();
        message.sessionId = sessionId;
        message.commandId = commandId;
        message.type = OutputType.START;
        message.content = "Executing command: " + command;
        message.timestamp = Instant.now();
        message.complete = false;
        return message;
    }

    // Constructor for end messages
    public static CommandOutputMessage createEndMessage(UUID sessionId, UUID commandId, int exitCode) {
        CommandOutputMessage message = new CommandOutputMessage();
        message.sessionId = sessionId;
        message.commandId = commandId;
        message.type = OutputType.END;
        message.content = "Command execution completed with exit code: " + exitCode;
        message.timestamp = Instant.now();
        message.complete = true;
        message.exitCode = exitCode;
        return message;
    }

    // Constructor for error messages
    public static CommandOutputMessage createErrorMessage(UUID sessionId, UUID commandId, String errorMessage) {
        CommandOutputMessage message = new CommandOutputMessage();
        message.sessionId = sessionId;
        message.commandId = commandId;
        message.type = OutputType.ERROR;
        message.content = "Error executing command: " + errorMessage;
        message.timestamp = Instant.now();
        message.complete = true;
        message.exitCode = -1;
        return message;
    }

    // Getters and setters
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    public OutputType getType() {
        return type;
    }

    public void setType(OutputType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }
}

