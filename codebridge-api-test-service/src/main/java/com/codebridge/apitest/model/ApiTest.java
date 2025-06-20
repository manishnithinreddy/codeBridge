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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProtocolType protocolType;

    @Column(nullable = false)
    private String url;

    @Column
    private String method;

    @Column
    @Lob
    private String headers;

    @Column
    @Lob
    private String body;

    @Column
    @Lob
    private String queryParams;

    @Column
    @Lob
    private String pathParams;

    @Column
    @Lob
    private String formParams;

    @Column
    private String authType;

    @Column
    @Lob
    private String authConfig;

    @Column
    @Lob
    private String assertions;

    @Column
    @Lob
    private String scripts;

    @Column
    private Integer timeout;

    @Column
    private Boolean followRedirects;

    // gRPC specific fields
    @Column
    private String grpcServiceName;
    
    @Column
    private String grpcMethodName;
    
    @Column
    @Lob
    private String grpcProtoDefinition;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
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

    // Add HttpMethod enum field
    @Column
    @Enumerated(EnumType.STRING)
    private HttpMethod httpMethod;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
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

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getPathParams() {
        return pathParams;
    }

    public void setPathParams(String pathParams) {
        this.pathParams = pathParams;
    }

    public String getFormParams() {
        return formParams;
    }

    public void setFormParams(String formParams) {
        this.formParams = formParams;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(String authConfig) {
        this.authConfig = authConfig;
    }

    public String getAssertions() {
        return assertions;
    }

    public void setAssertions(String assertions) {
        this.assertions = assertions;
    }

    public String getScripts() {
        return scripts;
    }

    public void setScripts(String scripts) {
        this.scripts = scripts;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Boolean getFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = followRedirects;
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

    public String getGrpcProtoDefinition() {
        return grpcProtoDefinition;
    }

    public void setGrpcProtoDefinition(String grpcProtoDefinition) {
        this.grpcProtoDefinition = grpcProtoDefinition;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }
}
