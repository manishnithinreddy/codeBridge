package com.codebridge.apitest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a test execution fails.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TestExecutionException extends RuntimeException {

    public TestExecutionException(String message) {
        super(message);
    }

    public TestExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

