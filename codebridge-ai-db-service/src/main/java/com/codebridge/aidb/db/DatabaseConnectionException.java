package com.codebridge.aidb.db.exception;

/**
 * Exception thrown when there is an issue with a database connection.
 */
public class DatabaseConnectionException extends RuntimeException {

    /**
     * Create a new DatabaseConnectionException with a message.
     *
     * @param message the error message
     */
    public DatabaseConnectionException(String message) {
        super(message);
    }

    /**
     * Create a new DatabaseConnectionException with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

