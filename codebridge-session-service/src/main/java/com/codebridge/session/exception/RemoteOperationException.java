package com.codebridge.session.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Default, can be overridden by specific handlers
public class RemoteOperationException extends RuntimeException {
    public RemoteOperationException(String message) {
        super(message);
    }

    public RemoteOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
