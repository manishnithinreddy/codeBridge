package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing; // Added import
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

// Import the actual SecurityConfig for api-test-service
// This assumes IncomingUserJwtConfigProperties is available or mocked if SecurityConfig depends on it.
// For WebMvcTest, often just the filter chain is applied, actual @Bean for JwtDecoder might not be fully invoked
// if not hit by the request, but good practice to import if it's light.
// For this test, we'll rely on the SecurityMockMvcRequestPostProcessors.jwt() to simulate authentication.
// @Import(SecurityConfig.class) // If SecurityConfig has light dependencies
@WebMvcTest(ProjectController.class)
class ProjectControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProjectService projectService;

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();
    private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR);

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR))
            .authorities(new SimpleGrantedAuthority("ROLE_USER")); // Add a default authority
    }

    @Test
    void createProject_authenticated_returnsCreated() throws Exception {
        ProjectRequest requestDto = new ProjectRequest();
        requestDto.setName("Test Project");
        requestDto.setDescription("Test Description");

        ProjectResponse responseDto = new ProjectResponse(UUID.randomUUID(), "Test Project", "Test Description", MOCK_USER_ID_UUID, LocalDateTime.now(), LocalDateTime.now());
        when(projectService.createProject(any(ProjectRequest.class), eq(MOCK_USER_ID_UUID))).thenReturn(responseDto);

        mockMvc.perform(post("/api/projects")
                .with(defaultUserJwt())
                .with(csrf()) // Include CSRF token if CSRF protection is enabled (default with Spring Security)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    void createProject_unauthenticated_returnsUnauthorized() throws Exception {
        ProjectRequest requestDto = new ProjectRequest();
        requestDto.setName("Test Project");
        mockMvc.perform(post("/api/projects")
                // No .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized());
    }


    @Test
    void getProjectById_authenticated_returnsProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectResponse responseDto = new ProjectResponse(projectId, "Test Project", "Desc", MOCK_USER_ID_UUID, LocalDateTime.now(), LocalDateTime.now());
        when(projectService.getProjectByIdForUser(projectId, MOCK_USER_ID_UUID)).thenReturn(responseDto);

        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                .with(defaultUserJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    void listProjects_authenticated_returnsProjectList() throws Exception {
        ProjectResponse responseDto = new ProjectResponse(UUID.randomUUID(), "Test Project", "Desc", MOCK_USER_ID_UUID, LocalDateTime.now(), LocalDateTime.now());
        List<ProjectResponse> projects = Collections.singletonList(responseDto);
        when(projectService.listProjectsForUser(MOCK_USER_ID_UUID)).thenReturn(projects);

        mockMvc.perform(get("/api/projects")
                .with(defaultUserJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Test Project"));
    }

    @Test
    void updateProject_authenticated_returnsUpdatedProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectRequest requestDto = new ProjectRequest();
        requestDto.setName("Updated Project");

        ProjectResponse responseDto = new ProjectResponse(projectId, "Updated Project", null, MOCK_USER_ID_UUID, LocalDateTime.now(), LocalDateTime.now());
        when(projectService.updateProject(eq(projectId), any(ProjectRequest.class), eq(MOCK_USER_ID_UUID))).thenReturn(responseDto);

        mockMvc.perform(put("/api/projects/{projectId}", projectId)
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Project"));
    }

    @Test
    void deleteProject_authenticated_returnsNoContent() throws Exception {
        UUID projectId = UUID.randomUUID();
        doNothing().when(projectService).deleteProject(projectId, MOCK_USER_ID_UUID);

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                .with(defaultUserJwt())
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
