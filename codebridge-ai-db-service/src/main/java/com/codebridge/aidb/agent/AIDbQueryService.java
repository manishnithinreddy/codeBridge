package com.codebridge.aidb.service;

import com.codebridge.aidb.client.AIServiceClient;
import com.codebridge.aidb.config.SessionServiceConfigProperties;
import com.codebridge.aidb.agent.NaturalLanguageQueryResponse;
import com.codebridge.aidb.agent.client.ClientSqlExecutionRequest;
import com.codebridge.aidb.agent.client.ClientSqlExecutionResponse;
import com.codebridge.aidb.agent.client.DbSchemaInfoResponse; // Local stub
import com.codebridge.aidb.agent.client.TableSchemaInfo;
import com.codebridge.aidb.agent.client.ColumnSchemaInfo;
import com.codebridge.aidb.exception.AIServiceException;
import com.codebridge.aidb.exception.InvalidSqlException;
import com.codebridge.aidb.util.SchemaToStringFormatter; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AIDbQueryService {

    private static final Logger logger = LoggerFactory.getLogger(AIDbQueryService.class);

    private final AIServiceClient aiServiceClient;
    private final WebClient.Builder webClientBuilder;
    private final SessionServiceConfigProperties sessionServiceConfigProperties;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SchemaToStringFormatter schemaFormatter; // Added
    private WebClient sessionServiceClient;

    public AIDbQueryService(AIServiceClient aiServiceClient,
                            WebClient.Builder webClientBuilder,
                            SessionServiceConfigProperties sessionServiceConfigProperties,
                            SqlSafetyValidator sqlSafetyValidator,
                            SchemaToStringFormatter schemaFormatter) { // Added
        this.aiServiceClient = aiServiceClient;
        this.webClientBuilder = webClientBuilder;
        this.sessionServiceConfigProperties = sessionServiceConfigProperties;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.schemaFormatter = schemaFormatter; // Added
    }

    @PostConstruct
    private void init() {
        this.sessionServiceClient = webClientBuilder
            .baseUrl(sessionServiceConfigProperties.getSessionServiceApiBaseUrl())
            .defaultHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    // Removed local formatSchemaForPrompt, will use SchemaToStringFormatter
    // private String formatSchemaForPrompt(DbSchemaInfoResponse schemaInfo) { ... }

    private String mapDbProductNameToDbType(String dbProductName) {
        if (dbProductName == null) return "SQL"; // Generic default
        String lowerProductName = dbProductName.toLowerCase(Locale.ROOT);
        if (lowerProductName.contains("postgres")) return "POSTGRESQL";
        if (lowerProductName.contains("mysql")) return "MYSQL";
        if (lowerProductName.contains("sql server")) return "SQLSERVER";
        if (lowerProductName.contains("oracle")) return "ORACLE";
        if (lowerProductName.contains("mariadb")) return "MARIADB";
        // Add more mappings as needed
        return dbProductName.toUpperCase(Locale.ROOT); // Fallback to product name if no specific mapping
    }

    private boolean isPotentiallyUnsafeSql(String sql) {
        if (sql == null) return true; // Treat null as unsafe
        String upperSql = sql.toUpperCase(Locale.ROOT);
        // Basic DML/DDL keyword check - this needs to be much more robust for production
        String[] restrictedKeywords = {"DROP", "DELETE", "UPDATE", "INSERT", "CREATE", "ALTER", "TRUNCATE", "GRANT", "REVOKE"};
        for (String keyword : restrictedKeywords) {
            if (upperSql.contains(keyword + " ") || upperSql.startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }


    public Mono<NaturalLanguageQueryResponse> processQuery(String dbSessionToken, String naturalLanguageQuery, UUID platformUserId) {
        NaturalLanguageQueryResponse finalResponse = new NaturalLanguageQueryResponse();

        // 1. Fetch Schema from SessionService
        return sessionServiceClient.get()
            .uri("/ops/db/{dbSessionToken}/get-schema-info", dbSessionToken)
            // .header("Authorization", "Bearer " + userJwtToken) // If SessionService /ops endpoint needs User JWT
            .retrieve()
            .bodyToMono(DbSchemaInfoResponse.class)
            .doOnError(e -> logger.error("Error fetching schema for session token {}: {}", dbSessionToken, e.getMessage()))
            .onErrorMap(e -> new AIServiceException("Failed to fetch database schema: " + e.getMessage(), e))
            .flatMap(schemaInfo -> {
                // 2. Call AIServiceClient
                String dbTypeForPrompt = mapDbProductNameToDbType(schemaInfo.getDatabaseProductName());
                String formattedSchema = schemaFormatter.formatSchema(schemaInfo, dbTypeForPrompt); // Use injected formatter

                return aiServiceClient.convertTextToSql(naturalLanguageQuery, dbTypeForPrompt, formattedSchema)
                    .flatMap(generatedSql -> {
                        finalResponse.setGeneratedSql(generatedSql);
                        logger.info("Generated SQL for token {}: {}", dbSessionToken, generatedSql);

                        // 3. SQL Validation/Sanitization
                        try {
                            // Assuming readOnly=true for AI generated queries by default for safety
                            sqlSafetyValidator.validateSqlQuery(generatedSql, true);
                            logger.info("AI-generated SQL passed safety validation: {}", generatedSql);
                        } catch (InvalidSqlException e) {
                            logger.warn("AI-generated SQL failed safety validation. SQL: [{}], Reason: {}", generatedSql, e.getMessage());
                            // Propagate as an error that GlobalExceptionHandler can catch
                            return Mono.error(e);
                        }

                        // 4. Execute SQL via SessionService
                        ClientSqlExecutionRequest sqlRequest = new ClientSqlExecutionRequest(generatedSql, true); // Default to readOnly=true

                        return sessionServiceClient.post()
                            .uri("/ops/db/{dbSessionToken}/execute-sql", dbSessionToken)
                            // .header("Authorization", "Bearer " + userJwtToken) // If SessionService /ops endpoint needs User JWT
                            .bodyValue(sqlRequest)
                            .retrieve()
                            .bodyToMono(ClientSqlExecutionResponse.class)
                            .map(sqlExecResult -> {
                                finalResponse.setSqlExecutionResult(sqlExecResult);
                                if (sqlExecResult.getError() != null) {
                                    finalResponse.setProcessingError("SQL execution failed: " + sqlExecResult.getError());
                                }
                                return finalResponse;
                            })
                            .doOnError(e -> logger.error("Error executing SQL via SessionService for token {}: {}", dbSessionToken, e.getMessage()))
                            .onErrorResume(e -> {
                                finalResponse.setProcessingError("Failed to execute SQL via SessionService: " + e.getMessage());
                                return Mono.just(finalResponse);
                            });
                    })
                    .doOnError(e -> {
                        if (e instanceof AIServiceException) {
                           logger.error("AI Service error for token {}: {}", dbSessionToken, e.getMessage());
                           finalResponse.setAiError(e.getMessage());
                        } else {
                           logger.error("Generic error after AI call for token {}: {}", dbSessionToken, e.getMessage());
                           finalResponse.setProcessingError(e.getMessage());
                        }
                    })
                    // Ensure response is returned even if AI part fails
                    .onErrorReturn(finalResponse); // Return current state of finalResponse on error from AI part
            })
            .onErrorResume(e -> { // Catch errors from schema fetch or subsequent flatMap stages
                logger.error("Error in processQuery for token {}: {}", dbSessionToken, e.getMessage());
                if (finalResponse.getAiError() == null && finalResponse.getProcessingError() == null) {
                    // If no specific error was set yet, set a general processing error
                    finalResponse.setProcessingError(e.getMessage());
                }
                return Mono.just(finalResponse);
            });
    }
}
