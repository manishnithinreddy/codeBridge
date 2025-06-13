package com.codebridge.aidb.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Using a record for ErrorDetails DTO, internal to this handler
    public record ErrorDetails(Date timestamp, String message, String details, Map<String, String> fieldErrors, String underlyingError) {
        public ErrorDetails(Date timestamp, String message, String details) {
            this(timestamp, message, details, null, null);
        }
         public ErrorDetails(Date timestamp, String message, String details, String underlyingError) {
            this(timestamp, message, details, null, underlyingError);
        }
        public ErrorDetails(Date timestamp, String message, String details, Map<String, String> fieldErrors) {
             this(timestamp, message, details, fieldErrors, null);
        }
    }

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ErrorDetails> handleAIServiceException(AIServiceException ex, WebRequest request) {
        logger.error("AI Service Exception: {}", ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        // Consider returning a more specific HTTP status if AI errors can be distinguished
        // e.g. BAD_GATEWAY if it's an issue calling the AI, or BAD_REQUEST if prompt was bad
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidSqlException.class) // Added handler
    public ResponseEntity<ErrorDetails> handleInvalidSqlException(InvalidSqlException ex, WebRequest request) {
        logger.warn("Invalid SQL Exception: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Invalid SQL: " + ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SpringAccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleSpringAccessDeniedException(SpringAccessDeniedException ex, WebRequest request) {
        logger.warn("Access Denied (Spring Security): {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Access Denied: You do not have permission to access this resource.", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication Failed (Spring Security): {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Authentication Failed: " + ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails(new Date(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // For @Valid DTO validation
    public ResponseEntity<ErrorDetails> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage()
                ));
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "Validation Failed", request.getDescription(false), fieldErrors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorDetails> handleWebClientResponseException(WebClientResponseException ex, WebRequest request) {
        logger.error("WebClient Response Error: Status {}, Body {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        ErrorDetails errorDetails = new ErrorDetails(
            new Date(),
            "Error during communication with a downstream service: " + ex.getStatusCode(),
            request.getDescription(false),
            ex.getResponseBodyAsString()
        );
        return new ResponseEntity<>(errorDetails, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class) // Generic catch-all
    public ResponseEntity<ErrorDetails> globalExceptionHandler(Exception ex, WebRequest request) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(new Date(), "An unexpected error occurred. Please contact support.", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
