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

    @Column
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApiTestType type;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpMethod method;

    @Column
    @Lob
    private String headers;

    @Column
    @Lob
    private String queryParams;

    @Column
    @Lob
    private String requestBody;

    @Column
    @Lob
    private String assertions;

    @Column
    private Integer timeout;

    @Column
    private Boolean followRedirects;

    @Column
    private Integer retryCount;

    @Column
    private Integer retryDelay;

    @Column
    @Enumerated(EnumType.STRING)
    private AuthType authType;

    @Column
    private String authUsername;

    @Column
    private String authPassword;

    @Column
    private String authToken;

    @Column
    @Lob
    private String preRequestScript;

    @Column
    @Lob
    private String postResponseScript;

    @Column
    @Lob
    private String environmentVariables;

    @Column
    @Lob
    private String graphqlQuery;

    @Column
    @Lob
    private String graphqlVariables;

    @Column
    @Lob
    private String websocketMessage;

    @Column
    private Integer websocketConnectionTimeout;

    @Column
    @Lob
    private String grpcServiceName;

    @Column
    @Lob
    private String grpcMethodName;

    @Column
    @Lob
    private String grpcRequest;

    @Column
    @Lob
    private String grpcProtoDefinition;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean enabled;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) {
            enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ApiTestType {
        HTTP,
        WEBSOCKET,
        GRPC,
        GRAPHQL
    }

    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        HEAD,
        OPTIONS
    }

    public enum AuthType {
        NONE,
        BASIC,
        BEARER,
        OAUTH2,
        API_KEY
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ApiTestType getType() {
        return type;
    }

    public void setType(ApiTestType type) {
        this.type = type;
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

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getAssertions() {
        return assertions;
    }

    public void setAssertions(String assertions) {
        this.assertions = assertions;
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

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Integer retryDelay) {
        this.retryDelay = retryDelay;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getPreRequestScript() {
        return preRequestScript;
    }

    public void setPreRequestScript(String preRequestScript) {
        this.preRequestScript = preRequestScript;
    }

    public String getPostResponseScript() {
        return postResponseScript;
    }

    public void setPostResponseScript(String postResponseScript) {
        this.postResponseScript = postResponseScript;
    }

    public String getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables;
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

    public String getWebsocketMessage() {
        return websocketMessage;
    }

    public void setWebsocketMessage(String websocketMessage) {
        this.websocketMessage = websocketMessage;
    }

    public Integer getWebsocketConnectionTimeout() {
        return websocketConnectionTimeout;
    }

    public void setWebsocketConnectionTimeout(Integer websocketConnectionTimeout) {
        this.websocketConnectionTimeout = websocketConnectionTimeout;
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

    public String getGrpcRequest() {
        return grpcRequest;
    }

    public void setGrpcRequest(String grpcRequest) {
        this.grpcRequest = grpcRequest;
    }

    public String getGrpcProtoDefinition() {
        return grpcProtoDefinition;
    }

    public void setGrpcProtoDefinition(String grpcProtoDefinition) {
        this.grpcProtoDefinition = grpcProtoDefinition;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

