package com.codebridge.session.exception;

public class RemoteOperationException extends RuntimeException {

    public RemoteOperationException(String message) {
        super(message);
    }

    public RemoteOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
