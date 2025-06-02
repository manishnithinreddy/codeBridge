package com.codebridge.server.controller;

import com.codebridge.server.dto.sessions.DbSessionCredentials;
import com.codebridge.server.dto.sessions.DbSessionInitRequest;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.dto.client.DbSessionServiceApiInitRequest; // DTO for calling session service
import com.codebridge.server.model.enums.DbType;
import com.codebridge.server.web.rest.errors.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DbSessionProxyController.class)
@Import(GlobalExceptionHandler.class) // Assuming a GlobalExceptionHandler exists
class DbSessionProxyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplateMock;

    private String sessionServiceBaseUrl = "http://fake-session-service/api"; // Default, will be set by ReflectionTestUtils
    private UUID fixedPlatformUserId;
    private DbSessionInitRequest controllerClientRequest; // Request coming to this controller

    @BeforeEach
    void setUp() {
        fixedPlatformUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        DbSessionCredentials credentials = new DbSessionCredentials();
        credentials.setDbType(DbType.POSTGRESQL);
        credentials.setHost("localhost");
        credentials.setPort(5432);
        credentials.setUsername("testuser");
        credentials.setPassword("testpass");
        credentials.setDatabaseName("testdb");

        controllerClientRequest = new DbSessionInitRequest();
        controllerClientRequest.setDbConnectionAlias("my_test_db_alias");
        controllerClientRequest.setCredentials(credentials);

        // Inject the base URL as it's a @Value field in the controller
        ReflectionTestUtils.setField(
            mockMvc.getDispatcherServlet().getWebApplicationContext().getBean(DbSessionProxyController.class),
            "sessionServiceBaseUrl",
            sessionServiceBaseUrl
        );
    }

    @Test
    void initializeDbSession_success_proxiesToSessionService() throws Exception {
        SessionResponse sessionServiceResponse = new SessionResponse(
            "jwt-from-session-service", 300000L, fixedPlatformUserId, UUID.randomUUID(), "DB:POSTGRESQL"
        );
        ResponseEntity<SessionResponse> responseEntity = new ResponseEntity<>(sessionServiceResponse, HttpStatus.CREATED);

        when(restTemplateMock.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/db/init"),
            any(DbSessionServiceApiInitRequest.class), // This DTO includes platformUserId
            eq(SessionResponse.class)
        )).thenReturn(responseEntity);

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(controllerClientRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").value("jwt-from-session-service"))
            .andExpect(jsonPath("$.userId").value(fixedPlatformUserId.toString()))
            .andExpect(jsonPath("$.resourceType").value("DB:POSTGRESQL"));
    }

    @Test
    void initializeDbSession_invalidClientRequest_returnsBadRequest() throws Exception {
        DbSessionInitRequest invalidClientRequest = new DbSessionInitRequest(); // Missing alias and credentials

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidClientRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void initializeDbSession_sessionServiceReturnsError_propagatesError() throws Exception {
        when(restTemplateMock.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/db/init"),
            any(DbSessionServiceApiInitRequest.class),
            eq(SessionResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Session Service Internal Error"));

        mockMvc.perform(post("/api/db/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(controllerClientRequest)))
            .andExpect(status().isInternalServerError()); // Assuming GlobalExceptionHandler maps this
    }


    @Test
    void keepAliveDbSession_success_proxiesToSessionService() throws Exception {
        String token = "db-session-token";
        KeepAliveResponse keepAliveServiceResponse = new KeepAliveResponse("new-db-jwt", 300000L);
        ResponseEntity<KeepAliveResponse> responseEntity = new ResponseEntity<>(keepAliveServiceResponse, HttpStatus.OK);

        when(restTemplateMock.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/db/" + token + "/keepalive"),
            any(), // HttpEntity can be null for POST with no body
            eq(KeepAliveResponse.class)
        )).thenReturn(responseEntity);

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/keepalive", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("new-db-jwt"));
    }

    @Test
    void keepAliveDbSession_sessionServiceReturnsNotFound_propagatesNotFound() throws Exception {
        String token = "unknown-db-token";
        when(restTemplateMock.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/db/" + token + "/keepalive"),
            any(),
            eq(KeepAliveResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Session Not Found by SessionService"));

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/keepalive", token))
            .andExpect(status().isNotFound());
    }

    @Test
    void releaseDbSession_success_proxiesToSessionService() throws Exception {
        String token = "db-token-to-release";
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        when(restTemplateMock.postForEntity(
            eq(sessionServiceBaseUrl + "/lifecycle/db/" + token + "/release"),
            any(),
            eq(Void.class)
        )).thenReturn(responseEntity);

        mockMvc.perform(post("/api/db/sessions/{sessionToken}/release", token))
            .andExpect(status().isNoContent());
    }
}
