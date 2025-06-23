package com.codebridge.server.exception;

/**
 * Exception thrown when an encryption or decryption operation cannot be performed.
 * This is typically used when Jasypt encryption operations fail.
 */
public class EncryptionOperationNotPossibleException extends RuntimeException {

    /**
     * Constructs a new encryption operation not possible exception with the specified detail message.
     *
     * @param message the detail message
     */
    public EncryptionOperationNotPossibleException(String message) {
        super(message);
    }

    /**
     * Constructs a new encryption operation not possible exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public EncryptionOperationNotPossibleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new encryption operation not possible exception with the specified cause.
     *
     * @param cause the cause
     */
    public EncryptionOperationNotPossibleException(Throwable cause) {
        super(cause);
    }
}

