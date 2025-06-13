package com.codebridge.server.controller;

import com.codebridge.server.dto.client.DbSessionCredentials;
import com.codebridge.server.dto.client.DbSessionServiceApiInitRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(DbSessionProxyController.class)
@TestPropertySource(properties = {"codebridge.service-urls.session-service=http://dummy-session-service/api/sessions"})
class DbSessionProxyControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RestTemplate restTemplate;
    // ServerAccessControlService is not directly used by DbSessionProxyController in current G.0 design
    // as DbSessionClientInitRequest contains all necessary info including credentials.

    @Value("${codebridge.service-urls.session-service}")
    private String sessionServiceBaseUrl;

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();
    private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR);

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void initDbSession_validRequest_proxiesToSessionService() throws Exception {
        DbSessionProxyController.DbSessionClientInitRequest clientRequest = new DbSessionProxyController.DbSessionClientInitRequest();
        clientRequest.dbConnectionAlias = "myPostgresDB";
        clientRequest.credentials = new DbSessionCredentials(); // Populate if needed for validation
        clientRequest.credentials.setDbType("POSTGRESQL"); // Assuming DbType is String here, or use Enum
        clientRequest.credentials.setHost("localhost");
        clientRequest.credentials.setPort(5432);
        clientRequest.credentials.setDatabaseName("testdb");
        clientRequest.credentials.setUsername("user");
        clientRequest.credentials.setPassword("pass");
        // clientRequest.platformUserId is not in DbSessionClientInitRequest, it's from JWT

        Map<String, Object> mockSessionServiceResponse = Map.of(
            "sessionToken", "mock-db-session-token",
            "type", "DB:POSTGRESQL",
            "status", "ACTIVE"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockSessionServiceResponse, HttpStatus.CREATED);

        when(restTemplate.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/db/init"),
            any(HttpEntity.class), // Check HttpEntity<DbSessionServiceApiInitRequestDto>
            eq(Map.class)))
            .thenReturn(responseEntity);

        mockMvc.perform(post("/api/sessions/db/init")
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").value("mock-db-session-token"));
    }

    @Test
    void keepAliveDbSession_proxiesToSessionService() throws Exception {
        String sessionToken = "mock-db-token-keepalive";
        Map<String, Object> mockSessionServiceResponse = Map.of("status", "ACTIVE", "sessionToken", sessionToken);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockSessionServiceResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(sessionServiceBaseUrl + "/lifecycle/db/" + sessionToken + "/keepalive"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)))
            .thenReturn(responseEntity);

        mockMvc.perform(post("/api/sessions/db/{sessionToken}/keepalive", sessionToken)
                .with(defaultUserJwt())
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void releaseDbSession_proxiesToSessionService() throws Exception {
        String sessionToken = "mock-db-token-release";
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        when(restTemplate.exchange(
            eq(sessionServiceBaseUrl + "/lifecycle/db/" + sessionToken + "/release"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)))
            .thenReturn(responseEntity);

        mockMvc.perform(post("/api/sessions/db/{sessionToken}/release", sessionToken)
                .with(defaultUserJwt())
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
