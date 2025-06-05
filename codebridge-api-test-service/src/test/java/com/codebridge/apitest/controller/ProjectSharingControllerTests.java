package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ShareGrantRequest;
import com.codebridge.apitest.dto.ShareGrantResponse;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.service.ProjectSharingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(ProjectSharingController.class)
class ProjectSharingControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProjectSharingService projectSharingService;

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString(); // This is the granter/revoker
    private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR);
    private final UUID projectId = UUID.randomUUID();
    private final UUID granteeUserId = UUID.randomUUID();


    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void grantProjectAccess_authenticated_returnsCreated() throws Exception {
        ShareGrantRequest requestDto = new ShareGrantRequest();
        requestDto.setGranteeUserId(granteeUserId);
        requestDto.setPermissionLevel(SharePermissionLevel.CAN_VIEW);

        ShareGrantResponse responseDto = new ShareGrantResponse(UUID.randomUUID(), projectId, "Test Project", granteeUserId, SharePermissionLevel.CAN_VIEW, MOCK_USER_ID_UUID, LocalDateTime.now());
        when(projectSharingService.grantProjectAccess(eq(projectId), any(ShareGrantRequest.class), eq(MOCK_USER_ID_UUID))).thenReturn(responseDto);

        mockMvc.perform(post("/api/projects/{projectId}/shares", projectId)
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.granteeUserId").value(granteeUserId.toString()));
    }

    @Test
    void grantProjectAccess_unauthenticated_returnsUnauthorized() throws Exception {
        ShareGrantRequest requestDto = new ShareGrantRequest();
        requestDto.setGranteeUserId(granteeUserId);
        requestDto.setPermissionLevel(SharePermissionLevel.CAN_VIEW);

        mockMvc.perform(post("/api/projects/{projectId}/shares", projectId)
                // No JWT
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeProjectAccess_authenticated_returnsNoContent() throws Exception {
        doNothing().when(projectSharingService).revokeProjectAccess(projectId, granteeUserId, MOCK_USER_ID_UUID);

        mockMvc.perform(delete("/api/projects/{projectId}/shares/users/{granteeUserId}", projectId, granteeUserId)
                .with(defaultUserJwt())
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void listSharedUsers_authenticated_returnsList() throws Exception {
        ShareGrantResponse responseDto = new ShareGrantResponse(UUID.randomUUID(), projectId, "Test Project", granteeUserId, SharePermissionLevel.CAN_VIEW, MOCK_USER_ID_UUID, LocalDateTime.now());
        List<ShareGrantResponse> shares = Collections.singletonList(responseDto);
        when(projectSharingService.listUsersForProject(projectId, MOCK_USER_ID_UUID)).thenReturn(shares);

        mockMvc.perform(get("/api/projects/{projectId}/shares", projectId)
                .with(defaultUserJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].granteeUserId").value(granteeUserId.toString()));
    }
}
