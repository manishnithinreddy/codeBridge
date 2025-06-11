package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.schema.DbSchemaInfoResponse;
import com.codebridge.session.dto.sql.SqlExecutionRequest;
import com.codebridge.session.model.DbSessionWrapper;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.DbSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
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
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DbSessionLifecycleManager dbLifecycleManager;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private ApplicationInstanceIdProvider instanceIdProvider;

    @Mock private Connection mockJdbcConnection;
    @Mock private DatabaseMetaData mockDatabaseMetaData;
    @Mock private DbSessionWrapper mockDbWrapper;

    private String validSessionToken = "valid-db-ops-token";
    private UUID platformUserId = UUID.randomUUID();
    private UUID resourceId = UUID.randomUUID();
    private SessionKey sessionKey;
    
    @BeforeEach
    void setUp() throws SQLException {
        sessionKey = new SessionKey(platformUserId, resourceId, "DB:POSTGRESQL");
        
        // Setup JWT token validation
        Claims claims = new DefaultClaims().setSubject(platformUserId.toString());
        claims.put("resourceId", resourceId.toString());
        claims.put("type", "DB:POSTGRESQL");
        
        when(jwtTokenProvider.validateToken(validSessionToken)).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken(validSessionToken)).thenReturn(claims);
        
        // Setup DB session
        DbSessionMetadata metadata = new DbSessionMetadata(
            sessionKey,
            Instant.now().toEpochMilli(),
            Instant.now().toEpochMilli(),
            Instant.now().toEpochMilli() + 3600000L,
            validSessionToken,
            "test-instance-1",
            "POSTGRESQL",
            "localhost",
            "testdb",
            "testuser"
        );
        
        when(mockDbWrapper.isValid(anyInt())).thenReturn(true);
        when(mockDbWrapper.getConnection()).thenReturn(mockJdbcConnection);
        when(mockJdbcConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        
        when(dbLifecycleManager.getLocalSession(sessionKey)).thenReturn(Optional.of(mockDbWrapper));
        when(dbLifecycleManager.getSessionMetadata(sessionKey)).thenReturn(Optional.of(metadata));
        when(instanceIdProvider.getInstanceId()).thenReturn("test-instance-1");
    }

    @Test
    void getSchemaInfo_shouldReturnSchemaInfo() throws Exception {
        // Setup mock response
        DbSchemaInfoResponse mockResponse = new DbSchemaInfoResponse();
        when(dbLifecycleManager.getSchemaInfo(any(DbSessionWrapper.class), anyString(), anyInt(), anyInt()))
                .thenReturn(mockResponse);

        // Perform request and validate
        mockMvc.perform(get("/api/ops/db/{sessionToken}/schema", validSessionToken)
                .param("schema", "public")
                .param("limit", "100")
                .param("offset", "0")
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

        // Create request
        SqlExecutionRequest request = new SqlExecutionRequest();
        request.setSql("SELECT * FROM users");

        // Perform request and validate
        mockMvc.perform(post("/api/ops/db/{sessionToken}/execute", validSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.columns").exists())
                .andExpect(jsonPath("$.rows").exists())
                .andExpect(jsonPath("$.rowCount").exists())
                .andExpect(jsonPath("$.executionTimeMs").exists());
    }
}

