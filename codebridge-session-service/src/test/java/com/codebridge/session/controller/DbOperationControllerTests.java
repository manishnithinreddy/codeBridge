package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.schema.DbSchemaInfoResponse;
import com.codebridge.session.model.DbSessionWrapper;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.DbSessionLifecycleManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DbOperationController.class)
class DbOperationControllerTests {

    @Autowired private MockMvc mockMvc;

    @MockBean private DbSessionLifecycleManager dbLifecycleManager;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private ApplicationInstanceIdProvider instanceIdProvider;

    @Mock private Connection mockJdbcConnection;
    @Mock private DatabaseMetaData mockDatabaseMetaData;
    @Mock private DbSessionWrapper mockDbWrapper;

    private String validSessionToken = "valid-db-ops-token";
    private UUID platformUserId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        
        // Setup JWT token validation
        Claims claims = new DefaultClaims();
        claims.put("userId", platformUserId.toString());
        claims.put("sessionId", "test-session-id");
        
        when(jwtTokenProvider.validateToken(validSessionToken)).thenReturn(true);
        when(jwtTokenProvider.getClaims(validSessionToken)).thenReturn(claims);
        
        // Setup DB session
        DbSessionMetadata metadata = new DbSessionMetadata();
        metadata.setSessionId(UUID.randomUUID());
        metadata.setUserId(platformUserId);
        metadata.setDbType(DbType.POSTGRESQL);
        
        when(mockDbWrapper.getMetadata()).thenReturn(metadata);
        when(mockDbWrapper.getConnection()).thenReturn(mockJdbcConnection);
        when(mockJdbcConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        
        SessionKey sessionKey = new SessionKey(metadata.getSessionId(), instanceIdProvider.getInstanceId());
        when(dbLifecycleManager.getSession(any(SessionKey.class))).thenReturn(Optional.of(mockDbWrapper));
    }

    @Test
    void getSchemaInfo_shouldReturnSchemaInfo() throws Exception {
        // Setup mock response
        DbSchemaInfoResponse mockResponse = new DbSchemaInfoResponse();
        when(dbLifecycleManager.getSchemaInfo(any(DbSessionWrapper.class), anyString(), anyInt(), anyInt()))
                .thenReturn(mockResponse);

        // Perform request and validate
        mockMvc.perform(get("/api/db/sessions/{sessionId}/schema", "test-session-id")
                .header("Authorization", "Bearer " + validSessionToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void executeQuery_shouldExecuteAndReturnResults() throws Exception {
        // Setup mock response
        Map<String, Object> mockResults = Map.of(
                "columns", new String[]{"id", "name"},
                "rows", new Object[]{Map.of("id", 1, "name", "test")},
                "rowCount", 1,
                "executionTimeMs", 10
        );
        
        when(dbLifecycleManager.executeQuery(any(DbSessionWrapper.class), anyString()))
                .thenReturn(mockResults);

        // Perform request and validate
        String requestBody = "{\"query\": \"SELECT * FROM users\"}";
        mockMvc.perform(post("/api/db/sessions/{sessionId}/query", "test-session-id")
                .header("Authorization", "Bearer " + validSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.columns").exists())
                .andExpect(jsonPath("$.rows").exists())
                .andExpect(jsonPath("$.rowCount").exists())
                .andExpect(jsonPath("$.executionTimeMs").exists());
    }
}

