package com.codebridge.aidb.client;

import com.codebridge.aidb.config.AiConfigProperties;
import com.codebridge.aidb.dto.ai.AiTextToSqlRequestDto;
import com.codebridge.aidb.dto.ai.AiTextToSqlResponseDto;
import com.codebridge.aidb.exception.AIServiceException;
// import com.codebridge.session.dto.schema.DbSchemaInfoResponse; // For later integration
// import com.codebridge.session.dto.schema.TableSchemaInfo;
// import com.codebridge.session.dto.schema.ColumnSchemaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

@Service
public class AIServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceClient.class);

    private final WebClient.Builder webClientBuilder;
    private final AiConfigProperties aiConfigProperties;
    private WebClient webClient;

    public AIServiceClient(WebClient.Builder webClientBuilder, AiConfigProperties aiConfigProperties) {
        this.webClientBuilder = webClientBuilder;
        this.aiConfigProperties = aiConfigProperties;
    }

    @PostConstruct
    private void init() {
        // The base URL for Gemini API is just the host, model is part of path
        // e.g., "https://generativelanguage.googleapis.com"
        // The full path is then specified in the .uri() method.
        // For now, let's assume endpointUrl in properties is the full path up to the model.
        // String baseUrl = aiConfigProperties.getEndpointUrl().substring(0, aiConfigProperties.getEndpointUrl().lastIndexOf("/v1beta"));
        this.webClient = webClientBuilder
            // .baseUrl(baseUrl) // Set base URL if endpointUrl is the full path
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    // Parameter changed to Object schemaInfoPlaceholder as per instructions
    public Mono<String> convertTextToSql(String naturalLanguageQuery, String dbType, Object schemaInfoPlaceholder) {
        logger.debug("Attempting to convert text to SQL. NLQ: '{}', DBType: '{}'", naturalLanguageQuery, dbType);
        logger.warn("Schema information (schemaInfoPlaceholder) is currently a placeholder and not used in prompt generation.");

        // TODO: Implement robust formatting of schemaInfoPlaceholder into the prompt string
        String formattedSchemaInfo = "SCHEMA_INFO_PLACEHOLDER"; // Actual schema formatting logic needed here
        if (schemaInfoPlaceholder != null) {
            // This is where you would iterate through tables and columns from the (future) DbSchemaInfoResponse
            // and build a string representation. For example:
            // StringBuilder sb = new StringBuilder();
            // for (TableSchemaInfo table : schemaInfoPlaceholder.getTables()) {
            //     sb.append(String.format("Table %s (%s):\n", table.getTableName(), table.getTableType()));
            //     for (ColumnSchemaInfo column : table.getColumns()) {
            //         sb.append(String.format("  - %s (%s, Nullable: %s)\n", column.getName(), column.getDataType(), column.isNullable()));
            //     }
            // }
            // formattedSchemaInfo = sb.toString();
            logger.info("Placeholder schema info type: {}", schemaInfoPlaceholder.getClass().getName());
        }


        String prompt = String.format(
            "Given the database type '%s' and the following database schema:\n%s\n" +
            "Generate a valid %s SQL query that answers the following user question:\n" +
            "User Question: \"%s\"\n" +
            "SQL Query:",
            dbType,
            formattedSchemaInfo,
            dbType,
            naturalLanguageQuery
        );

        logger.debug("Constructed prompt for AI: {}", prompt);

        AiTextToSqlRequestDto requestDto = new AiTextToSqlRequestDto(prompt);

        // Note: The actual Gemini API request structure is more complex (uses "contents" array).
        // This simplified DTO would need to be mapped to the correct Gemini structure before sending,
        // or the DTOs need to be made more complex. For now, sending simplified DTO.
        // The API key is typically sent as a query parameter like "?key=YOUR_API_KEY" appended to endpointUrl.

        return webClient.post()
            .uri(aiConfigProperties.getEndpointUrl() + "?key=" + aiConfigProperties.getApiKey())
            .bodyValue(requestDto) // This will be serialized to JSON
            .retrieve()
            .bodyToMono(AiTextToSqlResponseDto.class) // Expecting simplified response DTO
            .map(response -> {
                if (response.getGeneratedSql() != null && !response.getGeneratedSql().isBlank()) {
                    logger.info("Successfully converted text to SQL: {}", response.getGeneratedSql());
                    return response.getGeneratedSql();
                } else if (response.getError() != null) {
                    logger.error("AI service returned an error: {}", response.getError());
                    throw new AIServiceException("AI service returned an error: " + response.getError());
                }
                logger.error("Failed to extract SQL from AI response. Response was: {}", response);
                throw new AIServiceException("Failed to extract SQL from AI response. Response might be empty or malformed.");
            })
            .doOnError(error -> logger.error("AI Service call error during conversion: {}", error.getMessage(), error))
            .onErrorMap(e -> !(e instanceof AIServiceException), e -> new AIServiceException("Error calling AI service for Text-to-SQL: " + e.getMessage(), e));
    }
}
