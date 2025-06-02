package com.codebridge.session.controller;

import com.codebridge.session.config.DbSessionConfigProperties;
import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.dto.DbSessionCredentials;
import com.codebridge.session.dto.DbSessionInitRequest;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.exception.GlobalExceptionHandler;
import com.codebridge.session.service.DbSessionLifecycleManager;
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

@WebMvcTest(DbLifecycleController.class)
@Import({GlobalExceptionHandler.class, DbSessionConfigProperties.class, JwtConfigProperties.class})
class DbLifecycleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    @Qualifier("dbSessionManager") // Matches the qualifier in DbLifecycleController
    private DbSessionLifecycleManager dbSessionLifecycleManagerMock;

    private DbSessionInitRequest testInitRequest;
    private UUID testPlatformUserId; // This will now come from the request DTO

    @BeforeEach
    void setUp() {
        testPlatformUserId = UUID.randomUUID();
        DbSessionCredentials credentials = new DbSessionCredentials();
        credentials.setDbType(DbType.POSTGRESQL);
        credentials.setHost("localhost");
        credentials.setPort(5432);
        credentials.setUsername("user");
        credentials.setPassword("pass");
        credentials.setDatabaseName("db");

        testInitRequest = new DbSessionInitRequest(testPlatformUserId, "my_db_alias", credentials);
    }

    @Test
    void initializeDbSession_success() throws Exception {
        SessionResponse mockSessionResponse = new SessionResponse(
            "dummy-db-jwt", 300000L, testPlatformUserId, UUID.randomUUID(), "DB:POSTGRESQL"
        );
        when(dbSessionLifecycleManagerMock.initDbSession(
            eq(testPlatformUserId),
            eq(testInitRequest.getDbConnectionAlias()),
            any(DbSessionCredentials.class)))
            .thenReturn(mockSessionResponse);

        mockMvc.perform(post("/api/lifecycle/db/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInitRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sessionToken").value("dummy-db-jwt"))
            .andExpect(jsonPath("$.userId").value(testPlatformUserId.toString()))
            .andExpect(jsonPath("$.resourceType").value("DB:POSTGRESQL"));
    }

    @Test
    void initializeDbSession_invalidRequest_nullAlias() throws Exception {
        DbSessionInitRequest invalidRequest = new DbSessionInitRequest(testPlatformUserId, null, testInitRequest.getCredentials());

        mockMvc.perform(post("/api/lifecycle/db/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void initializeDbSession_invalidRequest_nullPlatformUserIdInDto() throws Exception {
        DbSessionInitRequest invalidRequest = new DbSessionInitRequest(null, "alias", testInitRequest.getCredentials());

        mockMvc.perform(post("/api/lifecycle/db/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void initializeDbSession_serviceThrowsRuntimeException() throws Exception {
        when(dbSessionLifecycleManagerMock.initDbSession(any(UUID.class), anyString(), any(DbSessionCredentials.class)))
            .thenThrow(new RuntimeException("Internal DB session setup failed"));

        mockMvc.perform(post("/api/lifecycle/db/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInitRequest)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void keepAliveDbSession_success() throws Exception {
        String validToken = "valid-db-token";
        KeepAliveResponse mockKeepAliveResponse = new KeepAliveResponse("new-db-jwt", 300000L);
        when(dbSessionLifecycleManagerMock.keepAliveDbSession(validToken)).thenReturn(Optional.of(mockKeepAliveResponse));

        mockMvc.perform(post("/api/lifecycle/db/{sessionToken}/keepalive", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("new-db-jwt"))
            .andExpect(jsonPath("$.expiresInMs").value(300000L));
    }

    @Test
    void keepAliveDbSession_notFoundOrInvalidToken() throws Exception {
        String invalidToken = "invalid-db-token";
        when(dbSessionLifecycleManagerMock.keepAliveDbSession(invalidToken)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/lifecycle/db/{sessionToken}/keepalive", invalidToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void releaseDbSession_success() throws Exception {
        String validToken = "valid-db-token-to-release";
        doNothing().when(dbSessionLifecycleManagerMock).releaseDbSession(validToken);

        mockMvc.perform(post("/api/lifecycle/db/{sessionToken}/release", validToken))
            .andExpect(status().isNoContent());
    }
}
