package com.codebridge.session.controller;

import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionServiceApiInitRequest;
import com.codebridge.session.dto.UserProvidedConnectionDetails;
import com.codebridge.session.model.enums.ServerAuthProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;


@WebMvcTest(SshLifecycleController.class)
class SshLifecycleControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SshSessionLifecycleManager sshLifecycleManager;
    // JwtTokenProvider and ApplicationInstanceIdProvider are not directly used by controller, but by manager
    // If Spring Security is fully active for WebMvcTest, may need to mock UserDetailsService or provide JWT
    // For this restoration, assuming basic @WebMvcTest setup.

    private final String MOCK_USER_ID = UUID.randomUUID().toString();

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultJwt() {
        return jwt().jwt(builder -> builder.subject(MOCK_USER_ID)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }


    @Test
    void initSshSession_validRequest_returnsCreated() throws Exception {
        UUID platformUserId = UUID.fromString(MOCK_USER_ID);
        UUID serverId = UUID.randomUUID();
        UserProvidedConnectionDetails connDetails = new UserProvidedConnectionDetails("host", 22, "user", ServerAuthProvider.PASSWORD);
        connDetails.setDecryptedPassword("password");

        SshSessionServiceApiInitRequest requestDto = new SshSessionServiceApiInitRequest(platformUserId, serverId, connDetails);

        SessionResponse sessionResponse = new SessionResponse("test-token", "SSH", "ACTIVE", System.currentTimeMillis(), System.currentTimeMillis() + 3600000);
        when(sshLifecycleManager.initSshSession(platformUserId, serverId, any(UserProvidedConnectionDetails.class)))
            .thenReturn(sessionResponse);

        mockMvc.perform(post("/api/lifecycle/ssh/init")
                .with(defaultJwt()) // Simulate authenticated user
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").value("test-token"))
            .andExpect(jsonPath("$.type").value("SSH"));
    }

    @Test
    void keepAliveSshSession_validToken_returnsOk() throws Exception {
        String sessionToken = "test-session-token";
        KeepAliveResponse keepAliveResponse = new KeepAliveResponse(sessionToken, "ACTIVE", System.currentTimeMillis() + 3600000);
        when(sshLifecycleManager.keepAliveSshSession(sessionToken)).thenReturn(keepAliveResponse);

        mockMvc.perform(post("/api/lifecycle/ssh/{sessionToken}/keepalive", sessionToken)
                .with(defaultJwt())) // Keepalive might also check original User JWT if configured
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value(sessionToken))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void releaseSshSession_validToken_returnsNoContent() throws Exception {
        String sessionToken = "test-session-token-release";
        // releaseSshSession is void
        doNothing().when(sshLifecycleManager).releaseSshSession(sessionToken);

        mockMvc.perform(post("/api/lifecycle/ssh/{sessionToken}/release", sessionToken)
                 .with(defaultJwt()))
            .andExpect(status().isNoContent());
    }
}
