package com.codebridge.aidb.client;

import com.codebridge.aidb.config.AiConfigProperties;
import com.codebridge.aidb.agent.ai.AiTextToSqlRequestDto;
import com.codebridge.aidb.agent.ai.AiTextToSqlResponseDto;
import com.codebridge.aidb.exception.AIServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceClientTests {

    @Mock private WebClient.Builder webClientBuilderMock;
    @Mock private WebClient webClientMock;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    @Mock private WebClient.RequestBodySpec requestBodySpecMock;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpecMock; // For bodyValue case
    @Mock private WebClient.ResponseSpec responseSpecMock;

    @Spy // Using Spy to allow partial mocking if needed, or specific field setting
    private AiConfigProperties aiConfigProperties = new AiConfigProperties();

    @InjectMocks
    private AIServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        aiConfigProperties.setEndpointUrl("https://fake-ai-service.com/generate");
        aiConfigProperties.setApiKey("test-key");

        // Initialize the WebClient within AIServiceClient manually after properties are set
        // This is because @PostConstruct init() is called after constructor but before tests might fully mock.
        // For a cleaner test, consider making WebClient injectable into AIServiceClient itself,
        // or having a setter for WebClient for test purposes.
        // For now, we re-initialize it or ensure the mocks intercept the builder.
        when(webClientBuilderMock.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilderMock);
        when(webClientBuilderMock.build()).thenReturn(webClientMock);

        // Call @PostConstruct method manually if not relying on Spring context for this unit test
        // This assumes AIServiceClient has an init method like the one shown in previous step
        // For simplicity, we'll assume webClientMock is used directly via the builder mock.
        // AIServiceClient will use the webClientMock returned by webClientBuilderMock.build()
        aiServiceClient.init(); // Manually call @PostConstruct if it sets up webClient from builder
                                // Or ensure webClientBuilderMock.build() returns webClientMock.

        // Standard mocking chain for WebClient
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock); // Use anyString() for URI with key
        when(requestBodySpecMock.bodyValue(any(AiTextToSqlRequestDto.class))).thenReturn(requestHeadersSpecMock); // Use requestHeadersSpecMock
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
    }

    @Test
    void convertTextToSql_success() {
        AiTextToSqlResponseDto mockResponse = new AiTextToSqlResponseDto("SELECT * FROM users;", null);
        when(responseSpecMock.bodyToMono(AiTextToSqlResponseDto.class)).thenReturn(Mono.just(mockResponse));

        Mono<String> resultMono = aiServiceClient.convertTextToSql("List all users", "POSTGRESQL", "Schema: users(id, name)");

        StepVerifier.create(resultMono)
            .expectNext("SELECT * FROM users;")
            .verifyComplete();

        verify(requestBodyUriSpecMock).uri("https://fake-ai-service.com/generate?key=test-key");
    }

    @Test
    void convertTextToSql_aiReturnsError_throwsAIServiceException() {
        AiTextToSqlResponseDto mockErrorResponse = new AiTextToSqlResponseDto(null, "AI processing error");
        when(responseSpecMock.bodyToMono(AiTextToSqlResponseDto.class)).thenReturn(Mono.just(mockErrorResponse));

        Mono<String> resultMono = aiServiceClient.convertTextToSql("Error query", "MYSQL", "Schema: products(id, price)");

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable -> throwable instanceof AIServiceException &&
                                             throwable.getMessage().contains("AI service returned an error: AI processing error"))
            .verify();
    }

    @Test
    void convertTextToSql_webClientError_throwsAIServiceException() {
        when(responseSpecMock.bodyToMono(AiTextToSqlResponseDto.class)).thenReturn(Mono.error(new RuntimeException("Network issue")));

        Mono<String> resultMono = aiServiceClient.convertTextToSql("Network error query", "SQLSERVER", "Schema: orders(id, amount)");

        StepVerifier.create(resultMono)
            .expectErrorMatches(throwable -> throwable instanceof AIServiceException &&
                                             throwable.getMessage().contains("Error calling AI service for Text-to-SQL: Network issue"))
            .verify();
    }
}
