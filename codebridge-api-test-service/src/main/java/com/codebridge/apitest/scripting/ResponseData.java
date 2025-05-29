package com.codebridge.apitest.scripting;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap; // Required for new HashMap<>()

public class ResponseData {
    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    public ResponseData(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        // Ensure headers are stored immutably if the input map is mutable
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers)); 
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
