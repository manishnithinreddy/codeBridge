package com.codebridge.session.exception; // Adapted package

import org.jasypt.exceptions.EncryptionOperationNotPossibleException; // Assuming this might be used if secrets are encrypted
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // Spring Security specific
import org.springframework.security.access.AccessDeniedException as SpringSecurityAccessDeniedException; // Spring Security specific
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.List; // Added missing import
import java.util.Map;
import java.util.stream.Collectors;

// Assuming custom exceptions like ResourceNotFoundException might be defined in this package too
// For now, let's include a basic one if not defined elsewhere.
// import com.codebridge.session.exception.ResourceNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Define ResourceNotFoundException if it's specific to this service and not in a shared lib
    // For now, this handler will work if such an exception is thrown.
    // If it's a shared exception, ensure the import is correct or it's available.
    @ExceptionHandler(ResourceNotFoundException.class) // Example: You might need to create this exception class
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        logger.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // Define AccessDeniedException if specific to this service
    @ExceptionHandler(AccessDeniedException.class) // Example: You might need to create this exception class
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        logger.warn("Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    // Handler for Spring Security's AuthenticationException (e.g. invalid JWT)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "Authentication failed: " + ex.getMessage(),
                request.getDescription(false));
        logger.warn("Authentication failure: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    // Handler for Spring Security's AccessDeniedException (e.g. insufficient permissions after successful auth)
    // This is different from our custom com.codebridge.session.exception.AccessDeniedException
    @ExceptionHandler(SpringSecurityAccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleSpringSecurityAccessDeniedException(SpringSecurityAccessDeniedException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "Access denied: " + ex.getMessage(),
                request.getDescription(false));
        logger.warn("Spring Security Access Denied: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RemoteOperationException.class)
    public ResponseEntity<ErrorDetails> handleRemoteOperationException(RemoteOperationException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(), // Message from RemoteOperationException
                request.getDescription(false));
        logger.error("Remote operation failed: {}", ex.getMessage(), ex); // Log with stack trace for ops errors
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR); // Or a more specific 5xx error
    }

    @ExceptionHandler(EncryptionOperationNotPossibleException.class)
    public ResponseEntity<ErrorDetails> handleEncryptionOperationNotPossibleException(EncryptionOperationNotPossibleException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "Encryption/Decryption error. Please check server configuration or input data.",
                request.getDescription(false));
        logger.error("Encryption operation failed: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        logger.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", new Date());
        body.put("status", HttpStatus.BAD_REQUEST.value());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        body.put("errors", errors);
        body.put("path", request.getDescription(false));

        logger.warn("Validation error: {}", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "An unexpected error occurred. Please try again later.",
                request.getDescription(false));
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static class ErrorDetails {
        private Date timestamp;
        private String message;
        private String details;

        public ErrorDetails(Date timestamp, String message, String details) {
            this.timestamp = timestamp;
            this.message = message;
            this.details = details;
        }

        public Date getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
    }

    // Basic ResourceNotFoundException (if not using a shared one)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
        public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
            super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        }
    }

    // Basic AccessDeniedException (if not using a shared one)
     public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
