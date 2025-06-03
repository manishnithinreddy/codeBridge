package com.codebridge.session.controller;

import com.codebridge.session.dto.DbSessionCredentials;
import com.codebridge.session.dto.DbSessionInitRequest;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.service.DbSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // For mock JWT
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors; // For mock JWT
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;


@WebMvcTest(DbLifecycleController.class)
class DbLifecycleControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DbSessionLifecycleManager dbLifecycleManager;
    // Note: DbLifecycleController's /init endpoint now expects an authenticated User JWT
    // because SessionServiceSecurityConfig secures /api/lifecycle/**

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void initDbSession_validRequest_returnsCreated() throws Exception {
        UUID platformUserId = UUID.fromString(MOCK_USER_ID_STR);
        String dbAlias = "myPostgresTest";
        DbSessionCredentials credentials = new DbSessionCredentials();
        credentials.setDbType(DbType.POSTGRESQL);
        credentials.setHost("localhost");
        credentials.setPort(5432);
        credentials.setDatabaseName("testdb");
        credentials.setUsername("user");
        credentials.setPassword("pass");

        DbSessionInitRequest requestDto = new DbSessionInitRequest(platformUserId, dbAlias, credentials);
        
        SessionResponse sessionResponse = new SessionResponse("db-test-token", "DB:POSTGRESQL", "ACTIVE", 
                                                            System.currentTimeMillis(), System.currentTimeMillis() + 1800000);
        when(dbLifecycleManager.initDbSession(platformUserId, dbAlias, any(DbSessionCredentials.class)))
            .thenReturn(sessionResponse);

        mockMvc.perform(post("/api/lifecycle/db/init")
                .with(defaultUserJwt()) // Send User JWT for authentication
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").value("db-test-token"))
            .andExpect(jsonPath("$.type").value("DB:POSTGRESQL"));
    }

    @Test
    void keepAliveDbSession_validToken_returnsOk() throws Exception {
        String sessionToken = "db-test-session-token";
        KeepAliveResponse keepAliveResponse = new KeepAliveResponse(sessionToken, "ACTIVE", System.currentTimeMillis() + 1800000);
        when(dbLifecycleManager.keepAliveDbSession(sessionToken)).thenReturn(keepAliveResponse);

        // Keepalive/release for DB sessions are secured by SessionServiceSecurityConfig with User JWT as well
        // if they fall under a generic .authenticated() rule, even though their primary auth is sessionToken.
        // For this test, let's assume they also need the User JWT like /init.
        mockMvc.perform(post("/api/lifecycle/db/{sessionToken}/keepalive", sessionToken)
                .with(defaultUserJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value(sessionToken))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void releaseDbSession_validToken_returnsNoContent() throws Exception {
        String sessionToken = "db-test-session-token-release";
        doNothing().when(dbLifecycleManager).releaseDbSession(sessionToken);

        mockMvc.perform(post("/api/lifecycle/db/{sessionToken}/release", sessionToken)
                .with(defaultUserJwt()))
            .andExpect(status().isNoContent());
    }
}
