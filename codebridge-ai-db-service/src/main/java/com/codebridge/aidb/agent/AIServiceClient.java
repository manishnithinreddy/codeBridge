package com.codebridge.aidb.client;

import com.codebridge.aidb.config.AiConfigProperties;
import com.codebridge.aidb.agent.ai.GeminiCandidateDto;
import com.codebridge.aidb.agent.ai.GeminiContentDto;
import com.codebridge.aidb.agent.ai.GeminiPartDto;
import com.codebridge.aidb.agent.ai.GeminiRequestPayloadDto;
import com.codebridge.aidb.agent.ai.AiTextToSqlResponseDto; // Ensure this is the updated one
import com.codebridge.aidb.exception.AIServiceException;
import java.util.List; // Added
// import com.codebridge.session.dto.schema.DbSchemaInfoResponse; // For later integration
// import com.codebridge.session.dto.schema.TableSchemaInfo;
// import com.codebridge.session.dto.schema.ColumnSchemaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders; // Added
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

    private String extractSqlFromText(String text) {
        if (text == null) return "";
        text = text.trim();

        String lowerText = text.toLowerCase();
        int sqlBlockStart = lowerText.indexOf("```sql");
        if (sqlBlockStart != -1) {
            int afterSqlBlockStart = sqlBlockStart + "```sql".length();
            int sqlBlockEnd = lowerText.indexOf("```", afterSqlBlockStart);
            if (sqlBlockEnd != -1) {
                return text.substring(afterSqlBlockStart, sqlBlockEnd).trim();
            } else { // No closing ``` found after ```sql
                return text.substring(afterSqlBlockStart).trim();
            }
        }
        // If no markdown SQL block, check for common SQL keywords at the beginning (simple check)
        String[] sqlKeywords = {"SELECT", "WITH", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP"};
        String upperText = text.toUpperCase();
        for (String keyword : sqlKeywords) {
            if (upperText.startsWith(keyword)) {
                return text; // Assume the whole text is SQL
            }
        }
        // If still not sure, and if it's a common pattern for the LLM to return "SQL Query: SELECT...", strip prefix
        if (upperText.startsWith("SQL QUERY:")) {
            return text.substring("SQL QUERY:".length()).trim();
        }

        logger.warn("Could not reliably extract SQL from AI response text. Returning raw text. Text: '{}'", text.substring(0, Math.min(text.length(), 200)));
        return text; // Fallback to returning the whole text, needs review
    }

    // Parameter 'schemaInfoPlaceholder' changed to 'String formattedSchema'
    public Mono<String> convertTextToSql(String naturalLanguageQuery, String dbType, String formattedSchema) {
        logger.debug("Attempting to convert text to SQL. NLQ: '{}', DBType: '{}'", naturalLanguageQuery, dbType);
        // formattedSchema is now directly passed as a string

        // Log a snippet of the schema for debugging, but be careful with potentially large schemas
        logger.debug("Using formatted schema for prompt (first 200 chars): {}",
                     formattedSchema != null ? formattedSchema.substring(0, Math.min(formattedSchema.length(), 200)) : "null");

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

        GeminiPartDto part = new GeminiPartDto(prompt);
        GeminiContentDto content = new GeminiContentDto(List.of(part));
        // content.setRole("user"); // Optional for single-turn
        GeminiRequestPayloadDto requestPayload = new GeminiRequestPayloadDto(List.of(content));

        // The API key is typically sent as a query parameter like "?key=YOUR_API_KEY" appended to endpointUrl.

        return webClient.post()
            .uri(aiConfigProperties.getEndpointUrl() + "?key=" + aiConfigProperties.getApiKey())
            .bodyValue(requestPayload) // Use the new Gemini-like request DTO
            .retrieve()
            .bodyToMono(AiTextToSqlResponseDto.class) // Use the updated response DTO
            .map(response -> {
                if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                    GeminiCandidateDto firstCandidate = response.getCandidates().get(0);
                    if (firstCandidate.getContent() != null &&
                        firstCandidate.getContent().getParts() != null &&
                        !firstCandidate.getContent().getParts().isEmpty()) {
                        String generatedText = firstCandidate.getContent().getParts().get(0).getText();
                        String extractedSql = extractSqlFromText(generatedText);
                        logger.info("Successfully converted text to SQL. Extracted: {}", extractedSql);
                        return extractedSql;
                    }
                }
                if (response.getError() != null) {
                    logger.error("AI service returned an error in response DTO: {}", response.getError());
                    throw new AIServiceException("AI service returned an error: " + response.getError());
                }
                logger.error("Failed to extract SQL from AI response. No suitable candidates/parts found. Response: {}", response);
                throw new AIServiceException("Failed to extract SQL from AI response. No suitable candidates found.");
            })
            .doOnError(error -> logger.error("AI Service call error during conversion: {}", error.getMessage(), error))
            .onErrorMap(e -> !(e instanceof AIServiceException), e -> new AIServiceException("Error calling AI service for Text-to-SQL: " + e.getMessage(), e));
    }
}
