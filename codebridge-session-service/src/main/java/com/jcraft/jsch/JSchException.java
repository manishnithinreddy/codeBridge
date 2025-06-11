package com.jcraft.jsch;

/**
 * Exception thrown by JSch.
 * This is a simplified version for test purposes.
 */
public class JSchException extends Exception {
    /**
     * Constructor for a JSch exception.
     */
    public JSchException() {
        super();
    }
    
    /**
     * Constructor for a JSch exception with a message.
     * @param message the message
     */
    public JSchException(String message) {
        super(message);
    }
    
    /**
     * Constructor for a JSch exception with a message and cause.
     * @param message the message
     * @param cause the cause
     */
    public JSchException(String message, Throwable cause) {
        super(message, cause);
    }
}

