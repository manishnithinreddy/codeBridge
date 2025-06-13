package com.codebridge.server.controller;

import com.codebridge.server.dto.UserSpecificConnectionDetailsDto;
import com.codebridge.server.dto.client.ClientUserProvidedConnectionDetails;
import com.codebridge.server.dto.client.SshSessionServiceApiInitRequestDto;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.service.ServerAccessControlService;
import com.codebridge.server.service.SshKeyManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(SshSessionController.class)
// Provide a dummy value for the required @Value annotation in SshSessionController
@TestPropertySource(properties = {"codebridge.service-urls.session-service=http://dummy-session-service/api/sessions"})
class SshSessionControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RestTemplate restTemplate;
    @MockBean private ServerAccessControlService serverAccessControlService;
    // SshKeyManagementService is used by the controller's initSshSession to prepare ClientUserProvidedConnectionDetails
    // This is not directly mocked here if we mock serverAccessControlService to return the full UserSpecificConnectionDetailsDto

    @Value("${codebridge.service-urls.session-service}")
    private String sessionServiceBaseUrl;

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();
    private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR);
    private final UUID serverId = UUID.randomUUID();

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        UserSpecificConnectionDetailsDto connDetailsDto = new UserSpecificConnectionDetailsDto(
            "localhost", 22, "testuser", ServerAuthProvider.SSH_KEY
        );
        SshKey mockKey = new SshKey();
        mockKey.setId(UUID.randomUUID());
        mockKey.setName("test-key");
        mockKey.setPrivateKey("decrypted-private-key-material"); // Decrypted by SshKeyManagementService
        connDetailsDto.setDecryptedSshKey(mockKey);

        when(serverAccessControlService.getValidatedConnectionDetails(MOCK_USER_ID_UUID, serverId))
            .thenReturn(connDetailsDto);
    }

    @Test
    void initSshSession_validRequest_proxiesToSessionService() throws Exception {
        SshSessionServiceApiInitRequestDto expectedRequestToSessionService = new SshSessionServiceApiInitRequestDto();
        // Populate expectedRequestToSessionService based on connDetailsDto and platformUserId, serverId

        Map<String, Object> mockSessionServiceResponse = Map.of(
            "sessionToken", "mock-session-token",
            "type", "SSH",
            "status", "ACTIVE"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockSessionServiceResponse, HttpStatus.CREATED);

        when(restTemplate.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/ssh/init"),
            any(HttpEntity.class),
            eq(Map.class)))
            .thenReturn(responseEntity);

        mockMvc.perform(post("/api/sessions/ssh/init")
                .param("serverId", serverId.toString())
                .with(defaultUserJwt())
                .with(csrf())) // If CSRF is enabled in tests (default for WebMvcTest if Spring Security is on classpath)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").value("mock-session-token"));
    }

    @Test
    void keepAliveSshSession_proxiesToSessionService() throws Exception {
        String sessionToken = "mock-ssh-token-keepalive";
        Map<String, Object> mockSessionServiceResponse = Map.of("status", "ACTIVE", "sessionToken", sessionToken);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockSessionServiceResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(sessionServiceBaseUrl + "/lifecycle/ssh/" + sessionToken + "/keepalive"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)))
            .thenReturn(responseEntity);

        mockMvc.perform(post("/api/sessions/ssh/{sessionToken}/keepalive", sessionToken)
                .with(defaultUserJwt()) // User JWT for the proxy call itself
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void releaseSshSession_proxiesToSessionService() throws Exception {
        String sessionToken = "mock-ssh-token-release";
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT); // Or OK with body

         when(restTemplate.exchange(
            eq(sessionServiceBaseUrl + "/lifecycle/ssh/" + sessionToken + "/release"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class))) // Assuming SessionService returns Map or Void
            .thenReturn(responseEntity);


        mockMvc.perform(post("/api/sessions/ssh/{sessionToken}/release", sessionToken)
                .with(defaultUserJwt())
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
