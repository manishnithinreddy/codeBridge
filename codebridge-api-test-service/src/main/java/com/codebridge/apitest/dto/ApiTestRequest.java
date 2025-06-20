package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.HttpMethod;
import com.codebridge.apitest.model.ProtocolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for API test operations.
 */
public class ApiTestRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;

    @NotNull(message = "Method cannot be null")
    private HttpMethod method;

    @NotNull(message = "Protocol cannot be null")
    private ProtocolType protocol;

    @NotBlank(message = "Endpoint cannot be blank")
    private String endpoint;

    private Map<String, String> requestHeaders;

    private Map<String, String> requestParams;

    private String requestBody;

    private List<Map<String, Object>> assertions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public ProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolType protocol) {
        this.protocol = protocol;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(Map<String, String> requestParams) {
        this.requestParams = requestParams;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public List<Map<String, Object>> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<Map<String, Object>> assertions) {
        this.assertions = assertions;
    }
}

