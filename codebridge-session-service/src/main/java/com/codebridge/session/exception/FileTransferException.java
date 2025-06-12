package com.codebridge.session.exception;

/**
 * Exception for file transfer errors
 */
public class FileTransferException extends RuntimeException {
    
    public FileTransferException(String message) {
        super(message);
    }
    
    public FileTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}

