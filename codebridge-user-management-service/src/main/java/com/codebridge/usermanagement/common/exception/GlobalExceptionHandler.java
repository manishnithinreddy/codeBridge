package com.codebridge.usermanagement.common.exception;

import com.codebridge.usermanagement.common.filter.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler that provides standardized error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle resource not found exception.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Resource not found: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, ex.getMessage(), path);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle bad credentials exception.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Bad credentials: {}", path);
        
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, "Invalid username or password", path);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle authentication exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Authentication failed: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED, "Authentication failed", path);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle access denied exception.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Access denied: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, "Access denied", path);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle validation exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Validation error: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Validation error", path);
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        apiError.addValidationErrors(errors);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Constraint violation: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Validation error", path);
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        
        apiError.addValidationErrors(errors);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle method argument type mismatch exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Type mismatch: {}", path, ex);
        
        String error = String.format("Parameter '%s' should be of type %s", 
                ex.getName(), ex.getRequiredType().getSimpleName());
        
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Type mismatch", path);
        apiError.addError(error);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle missing servlet request parameter exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Missing parameter: {}", path, ex);
        
        String error = String.format("Parameter '%s' of type %s is required", 
                ex.getParameterName(), ex.getParameterType());
        
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Missing parameter", path);
        apiError.addError(error);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle HTTP message not readable exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Malformed JSON request: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Malformed JSON request", path);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle data integrity violation exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("Data integrity violation: {}", path, ex);
        
        ApiError apiError = new ApiError(HttpStatus.CONFLICT, "Data integrity violation", path);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    /**
     * Handle no handler found exceptions.
     *
     * @param ex The exception
     * @param headers The headers
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, WebRequest request) {
        String path = getRequestPath(request);
        logger.warn("No handler found: {}", path, ex);
        
        String error = String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL());
        
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, "No handler found", path);
        apiError.addError(error);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle all other exceptions.
     *
     * @param ex The exception
     * @param request The request
     * @return The response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(
            Exception ex, WebRequest request) {
        String path = getRequestPath(request);
        logger.error("Unhandled exception: {}", path, ex);
        
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred", 
                path,
                ex);
        addCorrelationId(apiError);
        
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Gets the request path from the web request.
     *
     * @param request The web request
     * @return The request path
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    /**
     * Adds the correlation ID to the API error.
     *
     * @param apiError The API error
     */
    private void addCorrelationId(ApiError apiError) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId != null) {
            apiError.setCorrelationId(correlationId);
        }
    }
}
