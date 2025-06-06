package com.codebridge.aidb.exception;

// This exception can be mapped to HTTP 400 Bad Request by GlobalExceptionHandler
public class InvalidSqlException extends RuntimeException {
    public InvalidSqlException(String message) {
        super(message);
    }

    public InvalidSqlException(String message, Throwable cause) {
        super(message, cause);
    }
}
