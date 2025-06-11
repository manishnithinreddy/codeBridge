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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;


@WebMvcTest(SshLifecycleController.class)
class SshLifecycleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SshSessionLifecycleManager sshLifecycleManager;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .jwt(builder -> builder.claim("sub", UUID.randomUUID().toString()));
    }

    @Test
    void initSshSession_shouldReturnSessionResponse() throws Exception {
        // Prepare test data
        SshSessionServiceApiInitRequest request = new SshSessionServiceApiInitRequest();
        request.setConnectionAlias("test-connection");
        
        UserProvidedConnectionDetails details = new UserProvidedConnectionDetails();
        details.setHost("test-host");
        details.setPort(22);
        details.setUsername("test-user");
        details.setPassword("test-password");
        details.setAuthProvider(ServerAuthProvider.PASSWORD);
        
        request.setConnectionDetails(details);
        
        SessionResponse expectedResponse = new SessionResponse(
                "test-token", "SSH", "ACTIVE", 
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000);
        
        when(sshLifecycleManager.initSshSession(any(), any(), any()))
                .thenReturn(expectedResponse);

        // Perform request and validate
        mockMvc.perform(post("/api/lifecycle/ssh/init")
                .with(defaultJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value(expectedResponse.sessionToken()))
            .andExpect(jsonPath("$.type").value(expectedResponse.type()))
            .andExpect(jsonPath("$.status").value(expectedResponse.status()));
    }

    @Test
    void keepAliveSshSession_shouldReturnKeepAliveResponse() throws Exception {
        // Prepare test data
        String sessionToken = "test-token";
        KeepAliveResponse expectedResponse = new KeepAliveResponse(
                "new-test-token", "ACTIVE", System.currentTimeMillis() + 3600000);
        
        when(sshLifecycleManager.keepAliveSshSession(sessionToken))
                .thenReturn(expectedResponse);

        // Perform request and validate
        mockMvc.perform(post("/api/lifecycle/ssh/{sessionToken}/keepalive", sessionToken)
                .with(defaultJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value(expectedResponse.sessionToken()))
            .andExpect(jsonPath("$.status").value(expectedResponse.status()));
    }

    @Test
    void releaseSshSession_shouldReturnNoContent() throws Exception {
        // Prepare test data
        String sessionToken = "test-token";
        
        doNothing().when(sshLifecycleManager).releaseSshSession(sessionToken);

        mockMvc.perform(post("/api/lifecycle/ssh/{sessionToken}/release", sessionToken)
                 .with(defaultJwt()))
            .andExpect(status().isNoContent());
    }
}

