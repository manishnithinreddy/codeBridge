package com.codebridge.server.dto.remote;

public class CommandResponse {

    private String stdout;
    private String stderr;
    private Integer exitStatus;
    private long durationMs;

    public CommandResponse() {
    }

    public CommandResponse(String stdout, String stderr, Integer exitStatus, long durationMs) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitStatus = exitStatus;
        this.durationMs = durationMs;
    }

    // Getters and Setters
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
