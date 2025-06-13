package com.codebridge.server.exception;

// Can be annotated with @ResponseStatus if desired, or handled by GlobalExceptionHandler
public class FileTransferException extends RuntimeException {

    public FileTransferException(String message) {
        super(message);
    }

    public FileTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
