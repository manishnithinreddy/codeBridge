package com.codebridge.session.controller;

import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.dto.DbSchemaInfoResponse;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.exception.GlobalExceptionHandler;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.sessions.DbSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.ApplicationInstanceIdProvider;
import com.codebridge.session.service.DbSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DbOperationController.class)
@Import({GlobalExceptionHandler.class, JwtConfigProperties.class}) // DbSessionConfigProperties not directly needed by controller
class DbOperationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    @Qualifier("dbSessionManager")
    private DbSessionLifecycleManager dbSessionLifecycleManagerMock;
    @MockBean
    private JwtTokenProvider jwtTokenProviderMock;
    @MockBean
    private ApplicationInstanceIdProvider applicationInstanceIdProviderMock;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) // For DatabaseMetaData
    private Connection connectionMock;
    @Mock
    private DatabaseMetaData databaseMetaDataMock;


    private String testToken;
    private SessionKey testSessionKey;
    private DbSessionMetadata testMetadata;
    private DbSessionWrapper testDbSessionWrapper;
    private String testInstanceId = "test-db-ops-instance-id";

    @BeforeEach
    void setUp() throws SQLException { // Added SQLException for connectionMock.getMetaData()
        testToken = "valid.db.test.token";
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        testSessionKey = new SessionKey(userId, resourceId, "DB:POSTGRESQL");

        testMetadata = new DbSessionMetadata();
        testMetadata.setSessionKey(testSessionKey);
        testMetadata.setHostingInstanceId(testInstanceId);
        testMetadata.setApplicationInstanceId(testInstanceId); // Ensure this matches

        testDbSessionWrapper = mock(DbSessionWrapper.class);
        when(testDbSessionWrapper.getConnection()).thenReturn(connectionMock);
        when(testDbSessionWrapper.isValid(anyInt())).thenReturn(true); // Assume valid by default

        // Default mock behaviors for successful validation path in getValidatedLocalDbConnection helper
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testToken)).thenReturn(Optional.of(testSessionKey));
        when(dbSessionLifecycleManagerMock.getSessionMetadata(testSessionKey)).thenReturn(Optional.of(testMetadata));
        when(applicationInstanceIdProviderMock.getInstanceId()).thenReturn(testInstanceId);
        when(dbSessionLifecycleManagerMock.getLocalSession(testSessionKey)).thenReturn(Optional.of(testDbSessionWrapper));
        doNothing().when(dbSessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);

        // Mock for get-schema-info
        when(connectionMock.getMetaData()).thenReturn(databaseMetaDataMock);
    }

    // --- Test /test-connection ---
    @Test
    void testConnection_success() throws Exception {
        mockMvc.perform(post("/ops/db/{sessionToken}/test-connection", testToken))
            .andExpect(status().isOk())
            .andExpect(content().string("DB Connection test successful."));

        verify(dbSessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
    }

    @Test
    void testConnection_invalidToken_returnsForbidden() throws Exception {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey("invalid.token")).thenReturn(Optional.empty());
        mockMvc.perform(post("/ops/db/{sessionToken}/test-connection", "invalid.token"))
            .andExpect(status().isForbidden()) // AccessDeniedException
            .andExpect(jsonPath("$.message").value("Invalid, expired, or non-DB session token for test-connection."));
    }

    @Test
    void testConnection_sessionNotHostedLocally_returnsForbidden() throws Exception {
        when(applicationInstanceIdProviderMock.getInstanceId()).thenReturn("another-instance");
        mockMvc.perform(post("/ops/db/{sessionToken}/test-connection", testToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("DB Session not active on this service instance. Please retry or re-initialize."));
    }

    @Test
    void testConnection_localSessionWrapperInvalid_returnsForbiddenAndReleases() throws Exception {
        when(testDbSessionWrapper.isValid(anyInt())).thenReturn(false);

        mockMvc.perform(post("/ops/db/{sessionToken}/test-connection", testToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("DB Session connection is invalid. Please re-initialize."));

        verify(dbSessionLifecycleManagerMock).releaseDbSession(testToken); // Verify full release is triggered
    }


    // --- Test /get-schema-info ---
    @Test
    void getSchemaInfo_success() throws Exception {
        when(databaseMetaDataMock.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(databaseMetaDataMock.getDatabaseProductVersion()).thenReturn("15.0");
        when(databaseMetaDataMock.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(databaseMetaDataMock.getDriverVersion()).thenReturn("42.2.20");
        when(databaseMetaDataMock.getUserName()).thenReturn("testuser@testserver");
        when(databaseMetaDataMock.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        mockMvc.perform(get("/ops/db/{sessionToken}/get-schema-info", testToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.databaseProductName").value("PostgreSQL"))
            .andExpect(jsonPath("$.databaseProductVersion").value("15.0"))
            .andExpect(jsonPath("$.driverName").value("PostgreSQL JDBC Driver"))
            .andExpect(jsonPath("$.userName").value("testuser@testserver"));

        verify(dbSessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
    }

    @Test
    void getSchemaInfo_sqlException_returnsInternalServerErrorAndReleases() throws Exception {
        when(connectionMock.getMetaData()).thenThrow(new SQLException("Error getting metadata"));
        // Simulate session becoming invalid due to error
        when(testDbSessionWrapper.isValid(anyInt())).thenReturn(false);


        mockMvc.perform(get("/ops/db/{sessionToken}/get-schema-info", testToken))
            .andExpect(status().isInternalServerError()) // RemoteOperationException
            .andExpect(jsonPath("$.message").value("Failed to retrieve database metadata: Error getting metadata"));

        verify(dbSessionLifecycleManagerMock).releaseDbSession(testToken); // Verify session release
    }

    @Test
    void getSchemaInfo_invalidToken_returnsForbidden() throws Exception {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey("invalid-db-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/ops/db/{sessionToken}/get-schema-info", "invalid-db-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Invalid, expired, or non-DB session token for get-schema-info."));
    }
}
