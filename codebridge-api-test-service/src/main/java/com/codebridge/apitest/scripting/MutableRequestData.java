package com.codebridge.apitest.scripting;

import java.util.HashMap;
import java.util.Map;

public class MutableRequestData {
    private String url;
    private String method; // Ensure this can be modified if HttpMethod enum is used elsewhere
    private Map<String, String> headers;
    private String body;
    // GraphQL specific fields if needed, or handle them as part of the body
    // private String graphqlQuery;
    // private String graphqlVariables;


    public MutableRequestData(String url, String method, Map<String, String> headers, String body) {
        this.url = url;
        this.method = method;
        this.headers = new HashMap<>(headers); // Ensure mutable copy
        this.body = body;
    }

    // Getters
    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }

    // Setters
    public void setUrl(String url) { this.url = url; }
    public void setMethod(String method) { this.method = method; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; } // Or methods to add/remove individual headers
    public void setBody(String body) { this.body = body; }
}
