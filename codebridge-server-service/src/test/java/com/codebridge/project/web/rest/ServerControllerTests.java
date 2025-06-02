package com.codebridge.project.web.rest;

import com.codebridge.project.security.SecurityUtils;
import com.codebridge.project.service.ServerManagementService;
import com.codebridge.project.service.dto.ServerDTO;
import com.codebridge.project.service.dto.ServerDetailsDTO;
import com.codebridge.project.web.rest.errors.GlobalExceptionHandler;
import com.codebridge.project.service.dto.CreateServerRequest;
import com.codebridge.project.service.dto.UpdateServerRequest;
import com.codebridge.project.domain.enumeration.ServerAuthType;
import com.codebridge.project.domain.enumeration.ServerStatus;
import com.codebridge.project.service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServerController.class)
@Import(GlobalExceptionHandler.class)
class ServerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServerManagementService serverManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    private String mockAdminUserId;
    private String mockNonAdminUserId;


    @BeforeEach
    void setUp() {
        mockAdminUserId = UUID.randomUUID().toString();
        mockNonAdminUserId = UUID.randomUUID().toString();
        // Mock SecurityUtils.getCurrentUserId() if your controller uses it directly
        // For this example, we assume ServerManagementService handles user context or it's passed
    }

    private CreateServerRequest validCreatePasswordServerRequest() {
        CreateServerRequest request = new CreateServerRequest();
        request.setName("Test Server");
        request.setIpAddress("192.168.1.100");
        request.setPort(22);
        request.setAuthType(ServerAuthType.PASSWORD);
        request.setUsername("user");
        request.setPassword("password123");
        return request;
    }

    private CreateServerRequest validCreateSshKeyServerRequest() {
        CreateServerRequest request = new CreateServerRequest();
        request.setName("SSH Key Server");
        request.setIpAddress("192.168.1.101");
        request.setPort(22);
        request.setAuthType(ServerAuthType.SSH_KEY);
        request.setUsername("sshuser");
        request.setSshKeyId("ssh-key-id-123");
        return request;
    }

    private ServerDetailsDTO sampleServerDetailsDTO() {
        ServerDetailsDTO dto = new ServerDetailsDTO();
        dto.setId("server-id-123");
        dto.setName("Test Server");
        dto.setIpAddress("192.168.1.100");
        dto.setPort(22);
        dto.setUsername("user");
        dto.setAuthType(ServerAuthType.PASSWORD);
        dto.setStatus(ServerStatus.UNKNOWN);
        dto.setCreatedBy(mockAdminUserId);
        return dto;
    }

    @Test
    void createServer_withPasswordAuth_shouldReturnCreated() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        ServerDetailsDTO createdServer = sampleServerDetailsDTO();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.createServer(eq(mockAdminUserId), any(CreateServerRequest.class))).thenReturn(createdServer);

            mockMvc.perform(post("/api/servers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdServer.getId()))
                .andExpect(jsonPath("$.name").value(request.getName()));
        }
    }

    @Test
    void createServer_withSshKeyAuth_shouldReturnCreated() throws Exception {
        CreateServerRequest request = validCreateSshKeyServerRequest();
        ServerDetailsDTO createdServer = sampleServerDetailsDTO();
        createdServer.setAuthType(ServerAuthType.SSH_KEY);
        createdServer.setSshKeyId(request.getSshKeyId());


        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.createServer(eq(mockAdminUserId), any(CreateServerRequest.class))).thenReturn(createdServer);

            mockMvc.perform(post("/api/servers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdServer.getId()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.authType").value(ServerAuthType.SSH_KEY.toString()));
        }
    }

    @Test
    void createServer_whenNameIsBlank_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        request.setName(""); // Invalid

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").value("Server name cannot be blank."));
    }

    @Test
    void createServer_whenIpAddressIsInvalid_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        request.setIpAddress("invalid-ip"); // Invalid

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("ipAddress"))
            .andExpect(jsonPath("$.errors[0].message").value("Invalid IP address format."));
    }

    @Test
    void createServer_whenPortIsInvalid_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        request.setPort(0); // Invalid port

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("port"))
            .andExpect(jsonPath("$.errors[0].message").value("Port number must be between 1 and 65535."));
    }

    @Test
    void createServer_whenAuthTypeIsNull_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        request.setAuthType(null);

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("authType"))
            .andExpect(jsonPath("$.errors[0].message").value("Authentication type must be specified."));
    }

    @Test
    void createServer_passwordAuth_whenUsernameIsBlank_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        request.setUsername("");

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("username"))
            .andExpect(jsonPath("$.errors[0].message").value("Username is required for password authentication."));
    }

    @Test
    void createServer_passwordAuth_whenPasswordIsBlank_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreatePasswordServerRequest();
        request.setPassword("");

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("password"))
            .andExpect(jsonPath("$.errors[0].message").value("Password is required for password authentication."));
    }

    @Test
    void createServer_sshKeyAuth_whenUsernameIsBlank_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreateSshKeyServerRequest();
        request.setUsername("");

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("username"))
            .andExpect(jsonPath("$.errors[0].message").value("Username is required for SSH key authentication."));
    }

    @Test
    void createServer_sshKeyAuth_whenSshKeyIdIsBlank_shouldReturnBadRequest() throws Exception {
        CreateServerRequest request = validCreateSshKeyServerRequest();
        request.setSshKeyId("");

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("sshKeyId"))
            .andExpect(jsonPath("$.errors[0].message").value("SSH Key ID is required for SSH key authentication."));
    }

    @Test
    void createServer_whenSshKeyNotFound_shouldReturnNotFound() throws Exception {
        CreateServerRequest request = validCreateSshKeyServerRequest();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.createServer(eq(mockAdminUserId), any(CreateServerRequest.class)))
                .thenThrow(new ResourceNotFoundException("SSH Key not found"));

            mockMvc.perform(post("/api/servers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("SSH Key not found"));
        }
    }

    @Test
    void getServerById_shouldReturnServerDetails() throws Exception {
        String serverId = "server-id-123";
        ServerDetailsDTO serverDetails = sampleServerDetailsDTO();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.getServerById(eq(mockAdminUserId), eq(serverId))).thenReturn(serverDetails);

            mockMvc.perform(get("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serverId))
                .andExpect(jsonPath("$.name").value(serverDetails.getName()));
        }
    }

    @Test
    void getServerById_whenNotFound_shouldReturnNotFound() throws Exception {
        String serverId = "non-existent-server";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.getServerById(eq(mockAdminUserId), eq(serverId)))
                .thenThrow(new ResourceNotFoundException("Server not found"));

            mockMvc.perform(get("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Server not found"));
        }
    }

    @Test
    void listServersForUser_shouldReturnServerList() throws Exception {
        ServerDTO serverDTO = new ServerDTO();
        serverDTO.setId("server-id-1");
        serverDTO.setName("Server 1");
        List<ServerDTO> serverList = Collections.singletonList(serverDTO);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.listServersForUser(eq(mockAdminUserId))).thenReturn(serverList);

            mockMvc.perform(get("/api/servers")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("server-id-1"))
                .andExpect(jsonPath("$[0].name").value("Server 1"));
        }
    }

    @Test
    void listServersForUser_whenNoServers_shouldReturnEmptyList() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.listServersForUser(eq(mockAdminUserId))).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/servers")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    private UpdateServerRequest validUpdateServerRequest() {
        UpdateServerRequest request = new UpdateServerRequest();
        request.setName("Updated Server Name");
        request.setIpAddress("192.168.1.102");
        request.setPort(2222);
        request.setAuthType(ServerAuthType.PASSWORD);
        request.setUsername("newuser");
        request.setPassword("newpassword123");
        return request;
    }

    @Test
    void updateServer_shouldReturnUpdatedServerDetails() throws Exception {
        String serverId = "server-id-to-update";
        UpdateServerRequest request = validUpdateServerRequest();
        ServerDetailsDTO updatedServerDetails = sampleServerDetailsDTO();
        updatedServerDetails.setName(request.getName());
        updatedServerDetails.setIpAddress(request.getIpAddress());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.updateServer(eq(mockAdminUserId), eq(serverId), any(UpdateServerRequest.class)))
                .thenReturn(updatedServerDetails);

            mockMvc.perform(put("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedServerDetails.getId()))
                .andExpect(jsonPath("$.name").value(request.getName()));
        }
    }

    @Test
    void updateServer_whenNameIsBlank_shouldReturnBadRequest() throws Exception {
        String serverId = "server-id";
        UpdateServerRequest request = validUpdateServerRequest();
        request.setName("");

        mockMvc.perform(put("/api/servers/{serverId}", serverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").value("Server name cannot be blank."));
    }

    @Test
    void updateServer_whenServerNotFound_shouldReturnNotFound() throws Exception {
        String serverId = "non-existent-server";
        UpdateServerRequest request = validUpdateServerRequest();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.updateServer(eq(mockAdminUserId), eq(serverId), any(UpdateServerRequest.class)))
                .thenThrow(new ResourceNotFoundException("Server not found"));

            mockMvc.perform(put("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Server not found"));
        }
    }

    @Test
    void updateServer_whenSshKeyForAuthNotFound_shouldReturnNotFound() throws Exception {
        String serverId = "server-id";
        UpdateServerRequest request = validUpdateServerRequest();
        request.setAuthType(ServerAuthType.SSH_KEY);
        request.setSshKeyId("non-existent-key");
        request.setPassword(null); // SSH key auth

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverManagementService.updateServer(eq(mockAdminUserId), eq(serverId), any(UpdateServerRequest.class)))
                .thenThrow(new ResourceNotFoundException("SSH Key not found for update"));

            mockMvc.perform(put("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("SSH Key not found for update"));
        }
    }


    @Test
    void deleteServer_shouldReturnNoContent() throws Exception {
        String serverId = "server-id-to-delete";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            doNothing().when(serverManagementService).deleteServer(eq(mockAdminUserId), eq(serverId));

            mockMvc.perform(delete("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        }
    }

    @Test
    void deleteServer_whenNotFound_shouldReturnNotFound() throws Exception {
        String serverId = "non-existent-server";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            doThrow(new ResourceNotFoundException("Server not found for deletion"))
                .when(serverManagementService).deleteServer(eq(mockAdminUserId), eq(serverId));

            mockMvc.perform(delete("/api/servers/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Server not found for deletion"));
        }
    }
}
