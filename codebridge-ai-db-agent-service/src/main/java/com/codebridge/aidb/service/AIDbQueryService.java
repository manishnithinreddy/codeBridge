package com.codebridge.aidb.service;

import com.codebridge.aidb.client.AIServiceClient;
import com.codebridge.aidb.config.SessionServiceConfigProperties;
import com.codebridge.aidb.dto.NaturalLanguageQueryResponse;
import com.codebridge.aidb.dto.client.ClientSqlExecutionRequest;
import com.codebridge.aidb.dto.client.ClientSqlExecutionResponse;
import com.codebridge.aidb.dto.client.DbSchemaInfoResponse; // Local stub
import com.codebridge.aidb.dto.client.TableSchemaInfo; // Local stub
import com.codebridge.aidb.dto.client.ColumnSchemaInfo; // Local stub
import com.codebridge.aidb.exception.AIServiceException;
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
    private WebClient sessionServiceClient;

    public AIDbQueryService(AIServiceClient aiServiceClient,
                            WebClient.Builder webClientBuilder,
                            SessionServiceConfigProperties sessionServiceConfigProperties) {
        this.aiServiceClient = aiServiceClient;
        this.webClientBuilder = webClientBuilder;
        this.sessionServiceConfigProperties = sessionServiceConfigProperties;
    }

    @PostConstruct
    private void init() {
        this.sessionServiceClient = webClientBuilder
            .baseUrl(sessionServiceConfigProperties.getSessionServiceApiBaseUrl())
            .defaultHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    private String formatSchemaForPrompt(DbSchemaInfoResponse schemaInfo) {
        if (schemaInfo == null || schemaInfo.getTables() == null || schemaInfo.getTables().isEmpty()) {
            return "No schema information available.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Database Product: ").append(schemaInfo.getDatabaseProductName())
          .append(" Version: ").append(schemaInfo.getDatabaseProductVersion()).append("\n");

        for (TableSchemaInfo table : schemaInfo.getTables()) {
            sb.append(String.format("Table %s (%s):\n", table.getTableName(), table.getTableType()));
            if (table.getRemarks() != null && !table.getRemarks().isBlank()) {
                 sb.append(String.format("  Description: %s\n", table.getRemarks()));
            }
            for (ColumnSchemaInfo column : table.getColumns()) {
                sb.append(String.format("  - Column %s: Type=%s, Nullable=%s",
                                        column.getName(), column.getDataType(), column.isNullable()));
                if (column.getColumnSize() != null) sb.append(", Size=").append(column.getColumnSize());
                if (column.getDecimalDigits() != null) sb.append(", DecimalDigits=").append(column.getDecimalDigits());
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

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
                String dbType = mapDbProductNameToDbType(schemaInfo.getDatabaseProductName());
                String formattedSchema = formatSchemaForPrompt(schemaInfo);

                return aiServiceClient.convertTextToSql(naturalLanguageQuery, dbType, formattedSchema) // Pass formatted schema
                    .flatMap(generatedSql -> {
                        finalResponse.setGeneratedSql(generatedSql);
                        logger.info("Generated SQL for token {}: {}", dbSessionToken, generatedSql);

                        // 3. CRITICAL: SQL Validation/Sanitization Placeholder
                        // TODO: Implement robust SQL validation, sanitization, and restriction layer here!
                        if (isPotentiallyUnsafeSql(generatedSql)) {
                            logger.warn("Generated SQL contains restricted keywords: {}", generatedSql);
                            finalResponse.setProcessingError("Generated SQL contains restricted keywords and was not executed.");
                            // Alternatively, could throw new AIServiceException here
                            return Mono.just(finalResponse);
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
