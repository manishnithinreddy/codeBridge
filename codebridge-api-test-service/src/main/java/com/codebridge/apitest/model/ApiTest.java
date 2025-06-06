package com.codebridge.apitest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for API tests.
 * Supports HTTP, WebSocket, gRPC, and GraphQL requests.
 */
@Entity
@Table(name = "api_tests")
public class ApiTest {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private UUID userId;

    @Column
    private UUID teamId;
    
    @Column
    private UUID projectId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpMethod method;

    @Column
    @Enumerated(EnumType.STRING)
    private ProtocolType protocolType;

    @Column
    private UUID environmentId;

    @Column
    @Lob
    private String headers;

    @Column
    @Lob
    private String requestBody;

    @Column
    @Lob
    private String graphqlQuery;

    @Column
    @Lob
    private String graphqlVariables;

    @Column
    @Lob
    private String grpcRequest;

    @Column
    @Lob
    private String grpcServiceDefinition;

    @Column
    private String grpcServiceName;

    @Column
    private String grpcMethodName;

    @Column
    private Integer expectedStatusCode;

    @Column
    @Lob
    private String expectedResponseBody;

    @Column
    @Lob
    private String preRequestScript;

    @Column
    @Lob
    private String postRequestScript;

    @Column
    @Lob
    private String validationScript;

    @Column(nullable = false)
    private Integer timeoutMs;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
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

    public String getGrpcServiceName() {
        return grpcServiceName;
    }

    public void setGrpcServiceName(String grpcServiceName) {
        this.grpcServiceName = grpcServiceName;
    }

    public String getGrpcMethodName() {
        return grpcMethodName;
    }

    public void setGrpcMethodName(String grpcMethodName) {
        this.grpcMethodName = grpcMethodName;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
