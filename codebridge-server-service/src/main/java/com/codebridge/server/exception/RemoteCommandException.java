package com.codebridge.server.exception;

// Could also be annotated with @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) or similar
// if not handled by a global handler specifically.
public class RemoteCommandException extends RuntimeException {

    public RemoteCommandException(String message) {
        super(message);
    }

    public RemoteCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
