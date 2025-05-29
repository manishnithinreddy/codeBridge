package com.codebridge.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Or a more specific status if appropriate
public class RemoteCommandException extends RuntimeException {

    public RemoteCommandException(String message) {
        super(message);
    }

    public RemoteCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
