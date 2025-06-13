package com.codebridge.aidb.service;

import com.codebridge.aidb.config.SqlValidationConfigProperties;
import com.codebridge.aidb.exception.InvalidSqlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SqlSafetyValidator {

    private static final Logger logger = LoggerFactory.getLogger(SqlSafetyValidator.class);

    private final SqlValidationConfigProperties configProperties;
    private final Set<String> deniedKeywordsSet;

    // Pattern to find SQL comments (simple version, may not cover all edge cases)
    // Handles -- single line comments and /* ... */ multi-line comments
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("(--[^\\r\\n]*)|(/\\*.*?\\*/)", Pattern.DOTALL);


    public SqlSafetyValidator(SqlValidationConfigProperties configProperties) {
        this.configProperties = configProperties;
        this.deniedKeywordsSet = configProperties.getDeniedKeywordsSet();
        logger.info("SqlSafetyValidator initialized with denied keywords: {}", this.deniedKeywordsSet);
        logger.info("Allow only SELECT for read-only mode: {}", configProperties.isAllowOnlySelectForReadonly());
    }

    public void validateSqlQuery(String sqlQuery, boolean isReadOnlyIntended) throws InvalidSqlException {
        if (!StringUtils.hasText(sqlQuery)) {
            throw new IllegalArgumentException("SQL query cannot be empty.");
        }

        // Basic comment stripping
        String cleanedSql = SQL_COMMENT_PATTERN.matcher(sqlQuery).replaceAll(" ").trim();
        String upperCleanedSql = cleanedSql.toUpperCase(Locale.ROOT);

        logger.debug("Validating SQL (cleaned): {}", cleanedSql);

        // 1. Check for Denied Keywords
        for (String keyword : deniedKeywordsSet) {
            // Check for keyword as a whole word or followed by non-alphanumeric (e.g., space, parenthesis)
            // This is to avoid false positives on words like "UPDATEABLE" if "UPDATE" is denied.
            Pattern keywordPattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");
            if (keywordPattern.matcher(upperCleanedSql).find()) {
                logger.warn("Denied keyword '{}' found in query: {}", keyword, sqlQuery);
                throw new InvalidSqlException("Query contains restricted keyword: " + keyword);
            }
        }

        // 2. Enforce SELECT-only for Read-Only Mode
        if (isReadOnlyIntended && configProperties.isAllowOnlySelectForReadonly()) {
            String trimmedUpperSql = upperCleanedSql.trim();
            // Allow CTEs (WITH clause) before SELECT
            if (!trimmedUpperSql.startsWith("SELECT") && !trimmedUpperSql.startsWith("WITH")) {
                logger.warn("Non-SELECT query submitted in read-only mode: {}", sqlQuery);
                throw new InvalidSqlException("Only SELECT queries (optionally starting with WITH for CTEs) are allowed in read-only mode.");
            }
        }

        // 3. Basic check for multiple statements (naive version)
        // A query ending with a semicolon is fine, so count segments. More than 2 segments means multiple statements.
        // This doesn't properly handle semicolons in string literals or comments if not stripped perfectly.
        if (cleanedSql.split(";").length > 2) {
             logger.warn("Multiple SQL statements detected (naive check): {}", sqlQuery);
             throw new InvalidSqlException("Multiple SQL statements are not allowed in a single query.");
        }
        // A slightly better naive check: if there's a semicolon not at the very end (after trimming)
        String trimmedCleanedSql = cleanedSql.trim();
        int lastSemiColon = trimmedCleanedSql.lastIndexOf(';');
        if (lastSemiColon != -1 && lastSemiColon < trimmedCleanedSql.length() - 1) {
            logger.warn("Potential multiple SQL statements detected (semicolon not at end): {}", sqlQuery);
            throw new InvalidSqlException("Multiple SQL statements are not allowed (semicolon found mid-query).");
        }


        // TODO: Add more sophisticated checks for SQL injection patterns if possible,
        // e.g., tautologies, union-based attacks, etc. This is very complex.
        // Relying on prepared statements for parameter binding is the primary defense for actual execution.

        logger.info("SQL query passed basic safety validation: {}", sqlQuery.substring(0, Math.min(sqlQuery.length(), 100)) + "...");
    }
}
