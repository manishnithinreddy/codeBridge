package com.codebridge.server.controller;

import com.codebridge.server.config.DbSessionConfigProperties;
import com.codebridge.server.config.JwtConfigProperties;
import com.codebridge.server.dto.sessions.DbSessionCredentials;
import com.codebridge.server.dto.sessions.DbSessionInitRequest;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.model.enums.DbType;
import com.codebridge.server.sessions.RedisDbSessionManagerImpl;
import com.codebridge.server.web.rest.errors.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DbSessionController.class)
@Import({GlobalExceptionHandler.class, DbSessionConfigProperties.class, JwtConfigProperties.class})
class DbSessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mocking the concrete implementation that the controller uses.
    // The @Qualifier("dbSessionManager") in the controller's constructor will pick this up.
    @MockBean
    @Qualifier("dbSessionManager") // Ensure this matches the qualifier in the controller if it were using the interface
    private RedisDbSessionManagerImpl dbSessionManagerMock;


    private UUID fixedPlatformUserId; // From controller's getCurrentPlatformUserId()
    private String testDbConnectionAlias;

    @BeforeEach
    void setUp() {
        fixedPlatformUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        testDbConnectionAlias = "my_test_db_alias";
    }

    private DbSessionInitRequest validDbSessionInitRequest() {
        DbSessionInitRequest request = new DbSessionInitRequest();
        request.setDbConnectionAlias(testDbConnectionAlias);
        DbSessionCredentials credentials = new DbSessionCredentials();
        credentials.setDbType(DbType.POSTGRESQL);
        credentials.setHost("localhost");
        credentials.setPort(5432);
        credentials.setUsername("user");
        credentials.setPassword("pass");
        credentials.setDatabaseName("db");
        request.setCredentials(credentials);
        return request;
    }

    @Test
    void initializeDbSession_success() throws Exception {
        DbSessionInitRequest request = validDbSessionInitRequest();
        SessionResponse sessionResponseDto = new SessionResponse(
            "dummy-db-jwt", 600000L, fixedPlatformUserId,
            UUID.nameUUIDFromBytes((fixedPlatformUserId.toString() + ":" + testDbConnectionAlias).getBytes()), // matching resourceId generation
            "DB:" + DbType.POSTGRESQL.name()
        );

        when(dbSessionManagerMock.initDbSession(eq(fixedPlatformUserId), eq(testDbConnectionAlias), any(DbSessionCredentials.class)))
            .thenReturn(sessionResponseDto);

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sessionToken").value("dummy-db-jwt"))
            .andExpect(jsonPath("$.userId").value(fixedPlatformUserId.toString()))
            .andExpect(jsonPath("$.resourceType").value("DB:POSTGRESQL"));
    }

    @Test
    void initializeDbSession_invalidRequest_nullAlias() throws Exception {
        DbSessionInitRequest request = validDbSessionInitRequest();
        request.setDbConnectionAlias(null); // Invalid

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void initializeDbSession_invalidRequest_nullCredentials() throws Exception {
        DbSessionInitRequest request = new DbSessionInitRequest();
        request.setDbConnectionAlias(testDbConnectionAlias);
        request.setCredentials(null); // Invalid

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void initializeDbSession_invalidRequest_invalidCredentialsContent() throws Exception {
        DbSessionInitRequest request = validDbSessionInitRequest();
        request.getCredentials().setHost(null); // Invalid inner DTO

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }


    @Test
    void initializeDbSession_managerThrowsRuntimeException_globalExceptionHandlerHandles() throws Exception {
        DbSessionInitRequest request = validDbSessionInitRequest();
        when(dbSessionManagerMock.initDbSession(any(UUID.class), anyString(), any(DbSessionCredentials.class)))
            .thenThrow(new RuntimeException("DB connection failed internally"));

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void keepAliveDbSession_validToken_returnsOk() throws Exception {
        String validToken = "valid-db-jwt-token";
        KeepAliveResponse keepAliveDto = new KeepAliveResponse(validToken, 600000L);

        when(dbSessionManagerMock.keepAliveDbSession(eq(validToken)))
            .thenReturn(Optional.of(keepAliveDto));

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/keepalive", validToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value(validToken))
            .andExpect(jsonPath("$.expiresInMs").value(600000L));
    }

    @Test
    void keepAliveDbSession_invalidOrExpiredToken_returnsNotFound() throws Exception {
        String invalidToken = "invalid-db-jwt-token";
        when(dbSessionManagerMock.keepAliveDbSession(eq(invalidToken)))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/keepalive", invalidToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void releaseDbSession_validToken_returnsNoContent() throws Exception {
        String validToken = "valid-db-jwt-token-to-release";
        doNothing().when(dbSessionManagerMock).releaseDbSession(eq(validToken));

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/release", validToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void releaseDbSession_invalidToken_stillReturnsNoContent() throws Exception {
        String invalidToken = "invalid-db-token-for-release";
        doNothing().when(dbSessionManagerMock).releaseDbSession(eq(invalidToken));

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/release", invalidToken))
            .andExpect(status().isNoContent());
    }
}
