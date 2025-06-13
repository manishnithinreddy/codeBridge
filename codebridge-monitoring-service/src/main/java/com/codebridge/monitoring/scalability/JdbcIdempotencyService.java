package com.codebridge.monitoring.scalability.service.impl;

import com.codebridge.monitoring.scalability.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * JDBC-based implementation of the IdempotencyService.
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcIdempotencyService implements IdempotencyService {

    private final JdbcTemplate jdbcTemplate;
    private final int expirationHours;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> boolean recordKey(String key, T result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            Timestamp expiresAt = Timestamp.from(Instant.now().plus(expirationHours, ChronoUnit.HOURS));
            
            jdbcTemplate.update(
                "INSERT INTO idempotency_keys (key_value, result, created_at, expires_at) VALUES (?, ?, NOW(), ?)",
                key, resultJson, expiresAt
            );
            
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result for idempotency key: {}", key, e);
            throw new RuntimeException("Failed to serialize result", e);
        }
    }

    @Override
    public <T> Optional<T> getResult(String key, Class<T> resultClass) {
        try {
            String resultJson = jdbcTemplate.queryForObject(
                "SELECT result FROM idempotency_keys WHERE key_value = ? AND expires_at > NOW()",
                String.class,
                key
            );
            
            if (resultJson == null) {
                return Optional.empty();
            }
            
            T result = objectMapper.readValue(resultJson, resultClass);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Failed to retrieve result for idempotency key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String key) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM idempotency_keys WHERE key_value = ? AND expires_at > NOW()",
            Integer.class,
            key
        );
        
        return count != null && count > 0;
    }

    @Override
    public void removeKey(String key) {
        jdbcTemplate.update("DELETE FROM idempotency_keys WHERE key_value = ?", key);
    }
}

