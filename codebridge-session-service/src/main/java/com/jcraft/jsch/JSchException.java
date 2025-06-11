package com.jcraft.jsch;

/**
 * Exception thrown by JSch.
 * This is a simplified version for test purposes.
 */
public class JSchException extends Exception {
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructor for a JSch exception.
     */
    public JSchException() {
        super();
    }
    
    /**
     * Constructor for a JSch exception.
     * @param message the exception message
     */
    public JSchException(String message) {
        super(message);
    }
    
    /**
     * Constructor for a JSch exception.
     * @param message the exception message
     * @param cause the cause
     */
    public JSchException(String message, Throwable cause) {
        super(message, cause);
    }
}

