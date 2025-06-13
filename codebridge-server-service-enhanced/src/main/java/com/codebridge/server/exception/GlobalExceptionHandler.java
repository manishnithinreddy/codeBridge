package com.codebridge.server.exception;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException; // Added
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Custom Error Response Structure
    private static class ErrorDetails {
        private Date timestamp;
        private String message;
        private String details;
        private Map<String, String> fieldErrors; // For validation errors

        public ErrorDetails(Date timestamp, String message, String details) {
            super();
            this.timestamp = timestamp;
            this.message = message;
            this.details = details;
        }

        public ErrorDetails(Date timestamp, String message, String details, Map<String, String> fieldErrors) {
            this(timestamp, message, details);
            this.fieldErrors = fieldErrors;
        }

        // Getters (and potentially setters if needed by serialization)
        public Date getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
        public Map<String, String> getFieldErrors() { return fieldErrors; }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EncryptionOperationNotPossibleException.class)
    public ResponseEntity<?> jasyptEncryptionException(EncryptionOperationNotPossibleException ex, WebRequest request) {
        logger.error("Jasypt encryption/decryption error: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Encryption/Decryption error. Please check server configuration.", request.getDescription(false));
        // Could be BAD_REQUEST if it's due to bad input for decryption,
        // or INTERNAL_SERVER_ERROR if it's a configuration problem (e.g. wrong password)
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class) // Handles Spring Security's own AccessDeniedException
    public ResponseEntity<?> springAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access Denied by Spring Security: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Access Denied: You do not have permission to perform this action.", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class) // Added for Spring Security AuthenticationException
    public ResponseEntity<?> authenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Authentication Failed: " + ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    // Handler for @Valid annotation failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage()
                ));
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Validation Failed", request.getDescription(false), errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class) // Catch-all for now for things like auth principal not found
    public ResponseEntity<?> illegalStateException(IllegalStateException ex, WebRequest request) {
        logger.error("Illegal state: {}", ex.getMessage(), ex);
         ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        // This could be a 400 or 500 depending on context.
        // If it's "Authentication principal not found", it might indicate a misconfiguration or an unprotected route that needs it.
        // For now, treating as a client error (e.g. request made without auth when it's required).
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class) // Generic catch-all
    public ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "An unexpected error occurred.", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

