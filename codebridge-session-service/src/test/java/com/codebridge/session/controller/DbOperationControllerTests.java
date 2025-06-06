package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.ops.DbSchemaInfoResponse;
import com.codebridge.session.model.DbSessionWrapper;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.DbSessionLifecycleManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private UUID resourceId = UUID.randomUUID(); // Represents db alias hash
    private SessionKey sessionKey;

    @BeforeEach
    void setUp() throws SQLException {
        sessionKey = new SessionKey(platformUserId, resourceId, "DB:POSTGRESQL");
        Claims claims = new DefaultClaims().setSubject(platformUserId.toString());
        claims.put("resourceId", resourceId.toString());
        claims.put("type", "DB:POSTGRESQL");

        when(jwtTokenProvider.validateToken(validSessionToken)).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken(validSessionToken)).thenReturn(claims);

        DbSessionMetadata metadata = new DbSessionMetadata(sessionKey, System.currentTimeMillis(), System.currentTimeMillis(),
            System.currentTimeMillis() + 3600000, validSessionToken, "test-db-instance",
            "POSTGRESQL", "localhost", "testdb", "user");
        when(dbLifecycleManager.getSessionMetadata(sessionKey)).thenReturn(Optional.of(metadata));
        when(instanceIdProvider.getInstanceId()).thenReturn("test-db-instance");

        when(mockDbWrapper.isValid(anyInt())).thenReturn(true);
        when(mockDbWrapper.getConnection()).thenReturn(mockJdbcConnection);
        when(dbLifecycleManager.getLocalSession(sessionKey)).thenReturn(Optional.of(mockDbWrapper));

        when(mockJdbcConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
    }

    @Test
    void testConnection_validSession_returnsSuccess() throws Exception {
        when(mockJdbcConnection.isValid(anyInt())).thenReturn(true);

        mockMvc.perform(post("/ops/db/{sessionToken}/test-connection", validSessionToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Connection successful"));
    }

    @Test
    void getSchemaInfo_validSession_returnsSchemaInfo() throws Exception {
        when(mockDatabaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(mockDatabaseMetaData.getDatabaseProductVersion()).thenReturn("13.4");
        when(mockDatabaseMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(mockDatabaseMetaData.getDriverVersion()).thenReturn("42.2.20");

        mockMvc.perform(get("/ops/db/{sessionToken}/get-schema-info", validSessionToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.databaseProductName").value("PostgreSQL"))
            .andExpect(jsonPath("$.databaseProductVersion").value("13.4"));
    }
}
