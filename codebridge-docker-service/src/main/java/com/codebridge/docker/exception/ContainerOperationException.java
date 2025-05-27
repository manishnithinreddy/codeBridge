package com.codebridge.docker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a container operation fails.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ContainerOperationException extends RuntimeException {

    public ContainerOperationException(String message) {
        super(message);
    }

    public ContainerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

