package com.codebridge.monitoring.scalability.filter;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cached HTTP response for idempotent requests.
 */
@Data
public class IdempotencyResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The HTTP status code.
     */
    private int status;
    
    /**
     * The response headers.
     */
    private Map<String, String> headers = new HashMap<>();
    
    /**
     * The response body.
     */
    private String body;
}

