package com.codebridge.docker.model;

/**
 * Enum representing the possible states of a Docker container.
 */
public enum ContainerStatus {
    CREATED,
    RUNNING,
    PAUSED,
    RESTARTING,
    EXITED,
    DEAD,
    UNKNOWN
}

