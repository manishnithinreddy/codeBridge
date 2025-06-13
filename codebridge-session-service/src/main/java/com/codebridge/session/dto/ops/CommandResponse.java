package com.codebridge.session.dto.ops;

import java.io.Serializable;

public class CommandResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String commandId;
    private String command;
    private String stdout;
    private String stderr;
    private Integer exitStatus;
    private long durationMs;

    // Constructors
    public CommandResponse() {
    }

    public CommandResponse(String stdout, String stderr, Integer exitStatus, long durationMs) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitStatus = exitStatus;
        this.durationMs = durationMs;
    }

    public CommandResponse(String commandId, String command, String stdout, String stderr, Integer exitStatus, long durationMs) {
        this.commandId = commandId;
        this.command = command;
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitStatus = exitStatus;
        this.durationMs = durationMs;
    }

    // Getters and Setters
    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public Integer getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(Integer exitStatus) {
        this.exitStatus = exitStatus;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}

