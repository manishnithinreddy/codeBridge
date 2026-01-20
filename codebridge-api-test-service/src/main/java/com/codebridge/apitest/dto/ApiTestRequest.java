package com.codebridge.apitest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for API test operations.
 * Supports HTTP, WebSocket, gRPC, and GraphQL requests.
 */
public class ApiTestRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;

    @NotBlank(message = "URL cannot be blank")
    private String url;

    @NotBlank(message = "Method cannot be blank")
    @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS", message = "Method must be one of: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS")
    private String method;

    @Pattern(regexp = "HTTP|WEBSOCKET|GRPC|GRAPHQL", message = "Protocol type must be one of: HTTP, WEBSOCKET, GRPC, GRAPHQL")
    private String protocolType;

    private UUID environmentId;

    private Map<String, String> headers;

    private String requestBody;

    private String graphqlQuery;

    private String graphqlVariables;

    private String grpcRequest;

    private String grpcServiceDefinition;

    private Integer expectedStatusCode;

    private String expectedResponseBody;

    private String preRequestScript;

    private String postRequestScript;

    private String validationScript;

    @NotNull(message = "Timeout cannot be null")
    @Min(value = 1000, message = "Timeout must be at least 1000 ms")
    private Integer timeoutMs;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getGraphqlQuery() {
        return graphqlQuery;
    }

    public void setGraphqlQuery(String graphqlQuery) {
        this.graphqlQuery = graphqlQuery;
    }

    public String getGraphqlVariables() {
        return graphqlVariables;
    }

    public void setGraphqlVariables(String graphqlVariables) {
        this.graphqlVariables = graphqlVariables;
    }

    public String getGrpcRequest() {
        return grpcRequest;
    }

    public void setGrpcRequest(String grpcRequest) {
        this.grpcRequest = grpcRequest;
    }

    public String getGrpcServiceDefinition() {
        return grpcServiceDefinition;
    }

    public void setGrpcServiceDefinition(String grpcServiceDefinition) {
        this.grpcServiceDefinition = grpcServiceDefinition;
    }

    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(Integer expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getExpectedResponseBody() {
        return expectedResponseBody;
    }

    public void setExpectedResponseBody(String expectedResponseBody) {
        this.expectedResponseBody = expectedResponseBody;
    }

    public String getPreRequestScript() {
        return preRequestScript;
    }

    public void setPreRequestScript(String preRequestScript) {
        this.preRequestScript = preRequestScript;
    }

    public String getPostRequestScript() {
        return postRequestScript;
    }

    public void setPostRequestScript(String postRequestScript) {
        this.postRequestScript = postRequestScript;
    }

    public String getValidationScript() {
        return validationScript;
    }

    public void setValidationScript(String validationScript) {
        this.validationScript = validationScript;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
