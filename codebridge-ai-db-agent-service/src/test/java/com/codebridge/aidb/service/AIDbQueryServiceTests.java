package com.codebridge.aidb.service;

import com.codebridge.aidb.client.AIServiceClient;
import com.codebridge.aidb.config.SessionServiceConfigProperties;
import com.codebridge.aidb.dto.NaturalLanguageQueryResponse;
import com.codebridge.aidb.dto.client.ClientSqlExecutionRequest;
import com.codebridge.aidb.dto.client.ClientSqlExecutionResponse;
import com.codebridge.aidb.dto.client.DbSchemaInfoResponse; // Stub
import com.codebridge.aidb.exception.AIServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class AIDbQueryServiceTests {

    @Mock private AIServiceClient aiServiceClientMock;
    @Mock private WebClient.Builder webClientBuilderMock;
    @Mock private WebClient webClientMock; // For SessionService
    @Mock private SessionServiceConfigProperties sessionServiceConfigPropertiesMock;

    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpecMock;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock private WebClient.RequestBodySpec requestBodySpecMock;
    @Mock private WebClient.ResponseSpec responseSpecMock;


    @InjectMocks
    private AIDbQueryService aiDbQueryService;

    private String dbSessionToken = "db-session-token";
    private String nlQuery = "show all users";
    private UUID platformUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(sessionServiceConfigPropertiesMock.getSessionServiceApiBaseUrl()).thenReturn("http://dummy-session-service/api");

        // Mock WebClient for SessionService calls
        when(webClientBuilderMock.baseUrl(anyString())).thenReturn(webClientBuilderMock);
        when(webClientBuilderMock.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilderMock);
        when(webClientBuilderMock.build()).thenReturn(webClientMock);

        aiDbQueryService.init(); // Call @PostConstruct manually

        // Common WebClient mocking for SessionService calls
        when(webClientMock.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);

        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock); // Use requestHeadersSpecMock for bodyValue
        // when(requestBodySpecMock.retrieve()).thenReturn(responseSpecMock); // This was missing from AIServiceClient test, but might be needed if POST retrieve is different
    }

    @Test
    void processQuery_success() {
        DbSchemaInfoResponse schemaResponse = new DbSchemaInfoResponse();
        schemaResponse.setDatabaseProductName("PostgreSQL");
        // Populate with more schema details if needed for prompt formatting test
        when(responseSpecMock.bodyToMono(DbSchemaInfoResponse.class)).thenReturn(Mono.just(schemaResponse));

        String generatedSql = "SELECT * FROM users";
        when(aiServiceClientMock.convertTextToSql(eq(nlQuery), eq("POSTGRESQL"), anyString())).thenReturn(Mono.just(generatedSql));

        ClientSqlExecutionResponse sqlExecResponse = new ClientSqlExecutionResponse();
        sqlExecResponse.setColumnNames(List.of("id", "name"));
        sqlExecResponse.setRows(List.of(List.of(1, "User1")));
        // Mock the second retrieve() call for the SQL execution
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock); // Ensure this is mocked for the POST
        when(responseSpecMock.bodyToMono(ClientSqlExecutionResponse.class)).thenReturn(Mono.just(sqlExecResponse));

        Mono<NaturalLanguageQueryResponse> resultMono = aiDbQueryService.processQuery(dbSessionToken, nlQuery, platformUserId);

        StepVerifier.create(resultMono)
            .assertNext(response -> {
                assertEquals(generatedSql, response.getGeneratedSql());
                assertNotNull(response.getSqlExecutionResult());
                assertEquals(1, response.getSqlExecutionResult().getRows().size());
                assertNull(response.getAiError());
                assertNull(response.getProcessingError());
            })
            .verifyComplete();
    }

    @Test
    void processQuery_schemaFetchFails() {
        when(responseSpecMock.bodyToMono(DbSchemaInfoResponse.class)).thenReturn(Mono.error(new RuntimeException("Session service down")));

        Mono<NaturalLanguageQueryResponse> resultMono = aiDbQueryService.processQuery(dbSessionToken, nlQuery, platformUserId);

        StepVerifier.create(resultMono)
            .assertNext(response -> {
                assertNull(response.getGeneratedSql());
                assertNull(response.getSqlExecutionResult());
                assertTrue(response.getProcessingError().contains("Failed to fetch database schema"));
            })
            .verifyComplete();
    }

    @Test
    void processQuery_aiServiceFails() {
        DbSchemaInfoResponse schemaResponse = new DbSchemaInfoResponse(); // Assume schema fetch is OK
        schemaResponse.setDatabaseProductName("PostgreSQL");
        when(responseSpecMock.bodyToMono(DbSchemaInfoResponse.class)).thenReturn(Mono.just(schemaResponse));

        when(aiServiceClientMock.convertTextToSql(anyString(), anyString(), anyString()))
            .thenReturn(Mono.error(new AIServiceException("AI model unavailable")));

        Mono<NaturalLanguageQueryResponse> resultMono = aiDbQueryService.processQuery(dbSessionToken, nlQuery, platformUserId);

        StepVerifier.create(resultMono)
            .assertNext(response -> {
                assertNull(response.getGeneratedSql());
                assertNull(response.getSqlExecutionResult());
                assertTrue(response.getAiError().contains("AI model unavailable"));
            })
            .verifyComplete();
    }

    @Test
    void processQuery_sqlValidationFails_restrictedKeyword() {
        DbSchemaInfoResponse schemaResponse = new DbSchemaInfoResponse();
        schemaResponse.setDatabaseProductName("PostgreSQL");
        when(responseSpecMock.bodyToMono(DbSchemaInfoResponse.class)).thenReturn(Mono.just(schemaResponse));

        String maliciousSql = "DROP TABLE users;";
        when(aiServiceClientMock.convertTextToSql(anyString(), anyString(), anyString())).thenReturn(Mono.just(maliciousSql));

        Mono<NaturalLanguageQueryResponse> resultMono = aiDbQueryService.processQuery(dbSessionToken, nlQuery, platformUserId);

        StepVerifier.create(resultMono)
            .assertNext(response -> {
                assertEquals(maliciousSql, response.getGeneratedSql());
                assertNull(response.getSqlExecutionResult());
                assertTrue(response.getProcessingError().contains("restricted keywords"));
            })
            .verifyComplete();
    }


    @Test
    void processQuery_sqlExecutionFails() {
        DbSchemaInfoResponse schemaResponse = new DbSchemaInfoResponse();
        schemaResponse.setDatabaseProductName("PostgreSQL");
        when(responseSpecMock.bodyToMono(DbSchemaInfoResponse.class)).thenReturn(Mono.just(schemaResponse));

        String generatedSql = "SELECT * FROM non_existent_table";
        when(aiServiceClientMock.convertTextToSql(anyString(), anyString(), anyString())).thenReturn(Mono.just(generatedSql));

        ClientSqlExecutionResponse sqlExecErrorResponse = new ClientSqlExecutionResponse();
        sqlExecErrorResponse.setError("Table not found");
        // Mock the second retrieve() call for the SQL execution
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(ClientSqlExecutionResponse.class)).thenReturn(Mono.just(sqlExecErrorResponse));

        Mono<NaturalLanguageQueryResponse> resultMono = aiDbQueryService.processQuery(dbSessionToken, nlQuery, platformUserId);

        StepVerifier.create(resultMono)
            .assertNext(response -> {
                assertEquals(generatedSql, response.getGeneratedSql());
                assertNotNull(response.getSqlExecutionResult());
                assertTrue(response.getSqlExecutionResult().getError().contains("Table not found"));
                assertTrue(response.getProcessingError().contains("SQL execution failed"));
            })
            .verifyComplete();
    }
}
