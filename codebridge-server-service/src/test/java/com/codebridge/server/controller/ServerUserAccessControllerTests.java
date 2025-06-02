package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerUserRequest;
import com.codebridge.server.dto.ServerUserResponse;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.GlobalExceptionHandler;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.service.ServerAccessControlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;


@WebMvcTest(ServerUserAccessController.class)
@Import(GlobalExceptionHandler.class) // Ensure our global exception handler is used
class ServerUserAccessControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServerAccessControlService serverAccessControlService;

    @Autowired
    private ObjectMapper objectMapper;

    // To mock getCurrentUserId behavior if Spring Security context is not fully available/mocked
    // This is a simplified approach. In a full Spring Security test, you'd use @WithMockUser or similar.
    // For this controller, the placeholder `getCurrentUserId()` returns a fixed UUID. We will assume this.
    private UUID mockAdminUserId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");


    private ServerUserRequest serverUserRequest;
    private ServerUserResponse serverUserResponse;
    private UUID serverId;
    private UUID targetPlatformUserId;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        // Can also initialize mockMvc using MockMvcBuilders if more customization is needed
        // mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        serverId = UUID.randomUUID();
        targetPlatformUserId = UUID.randomUUID();
        UUID sshKeyId = UUID.randomUUID();

        serverUserRequest = new ServerUserRequest();
        serverUserRequest.setServerId(serverId);
        serverUserRequest.setPlatformUserId(targetPlatformUserId);
        serverUserRequest.setRemoteUsernameForUser("testRemoteUser");
        serverUserRequest.setSshKeyIdForUser(sshKeyId);

        serverUserResponse = new ServerUserResponse();
        serverUserResponse.setId(UUID.randomUUID());
        serverUserResponse.setServerId(serverId);
        serverUserResponse.setServerName("TestServer");
        serverUserResponse.setPlatformUserId(targetPlatformUserId);
        serverUserResponse.setRemoteUsernameForUser("testRemoteUser");
        serverUserResponse.setSshKeyIdForUser(sshKeyId);
        serverUserResponse.setSshKeyNameForUser("TestKey");
        serverUserResponse.setAccessGrantedBy(mockAdminUserId);
        serverUserResponse.setCreatedAt(LocalDateTime.now());
        serverUserResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void grantServerAccess_success() throws Exception {
        when(serverAccessControlService.grantServerAccess(eq(mockAdminUserId), any(ServerUserRequest.class)))
                .thenReturn(serverUserResponse);

        mockMvc.perform(post("/api/server-access/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serverUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(serverUserResponse.getId().toString())))
                .andExpect(jsonPath("$.platformUserId", is(targetPlatformUserId.toString())))
                .andExpect(jsonPath("$.remoteUsernameForUser", is("testRemoteUser")));
    }

    @Test
    void grantServerAccess_validationError_nullServerId() throws Exception {
        serverUserRequest.setServerId(null); // Invalid input

        mockMvc.perform(post("/api/server-access/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serverUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", is("serverId: Server ID cannot be null")));
    }

    @Test
    void grantServerAccess_serverNotFound_returns404() throws Exception {
        when(serverAccessControlService.grantServerAccess(eq(mockAdminUserId), any(ServerUserRequest.class)))
            .thenThrow(new ResourceNotFoundException("Server", "id", serverUserRequest.getServerId()));

        mockMvc.perform(post("/api/server-access/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serverUserRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Server not found with id : '" + serverUserRequest.getServerId() + "'")));
    }

    @Test
    void grantServerAccess_adminNotOwner_returns403() throws Exception {
        when(serverAccessControlService.grantServerAccess(eq(mockAdminUserId), any(ServerUserRequest.class)))
            .thenThrow(new AccessDeniedException("Admin user does not have rights"));

        mockMvc.perform(post("/api/server-access/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serverUserRequest)))
                .andExpect(status().isForbidden())
                 .andExpect(jsonPath("$.message", is("Admin user does not have rights")));
    }


    @Test
    void revokeServerAccess_success() throws Exception {
        doNothing().when(serverAccessControlService).revokeServerAccess(mockAdminUserId, serverId, targetPlatformUserId);

        mockMvc.perform(delete("/api/server-access/grants/servers/{serverId}/users/{targetPlatformUserId}",
                        serverId, targetPlatformUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    void revokeServerAccess_grantNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("ServerUser access grant not found"))
            .when(serverAccessControlService).revokeServerAccess(mockAdminUserId, serverId, targetPlatformUserId);

        mockMvc.perform(delete("/api/server-access/grants/servers/{serverId}/users/{targetPlatformUserId}",
                        serverId, targetPlatformUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("ServerUser access grant not found")));
    }

    @Test
    void revokeServerAccess_adminNotOwner_returns403() throws Exception {
        doThrow(new AccessDeniedException("Admin user does not have rights"))
            .when(serverAccessControlService).revokeServerAccess(mockAdminUserId, serverId, targetPlatformUserId);

        mockMvc.perform(delete("/api/server-access/grants/servers/{serverId}/users/{targetPlatformUserId}",
                        serverId, targetPlatformUserId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Admin user does not have rights")));
    }
}
