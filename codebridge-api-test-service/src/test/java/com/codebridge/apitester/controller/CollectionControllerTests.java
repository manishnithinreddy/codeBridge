package com.codebridge.apitester.controller;

import com.codebridge.apitester.dto.CollectionRequest;
import com.codebridge.apitester.dto.CollectionResponse;
import com.codebridge.apitester.service.CollectionService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(CollectionController.class)
class CollectionControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CollectionService collectionService;

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();
    private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR);
    private final UUID projectId = UUID.randomUUID();

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void createCollection_authenticated_returnsCreated() throws Exception {
        CollectionRequest requestDto = new CollectionRequest();
        requestDto.setName("Test Collection");
        // projectId is set from path in controller, but DTO might also carry it for service
        requestDto.setProjectId(projectId); 

        CollectionResponse responseDto = new CollectionResponse(); // Populate as needed
        responseDto.setId(UUID.randomUUID());
        responseDto.setName("Test Collection");
        responseDto.setProjectId(projectId);

        // Service method takes CollectionRequest (which now has projectId set by controller) and platformUserId
        when(collectionService.createCollection(any(CollectionRequest.class), eq(MOCK_USER_ID_UUID))).thenReturn(responseDto);

        mockMvc.perform(post("/api/projects/{projectId}/collections", projectId)
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Collection"))
            .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }
    
    @Test
    void createCollection_unauthenticated_returnsUnauthorized() throws Exception {
         CollectionRequest requestDto = new CollectionRequest();
        requestDto.setName("Test Collection");
        requestDto.setProjectId(projectId); 

        mockMvc.perform(post("/api/projects/{projectId}/collections", projectId)
                // No JWT
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getCollectionById_authenticated_returnsCollection() throws Exception {
        UUID collectionId = UUID.randomUUID();
        CollectionResponse responseDto = new CollectionResponse();
        responseDto.setId(collectionId);
        responseDto.setName("Test Collection");
        responseDto.setProjectId(projectId); // Important for the check in controller
        
        when(collectionService.getCollectionByIdForUser(collectionId, MOCK_USER_ID_UUID)).thenReturn(responseDto);

        mockMvc.perform(get("/api/projects/{projectId}/collections/{collectionId}", projectId, collectionId)
                .with(defaultUserJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(collectionId.toString()));
    }

    @Test
    void getAllCollectionsForProject_authenticated_returnsList() throws Exception {
        CollectionResponse responseDto = new CollectionResponse();
        responseDto.setId(UUID.randomUUID());
        responseDto.setName("Test Collection");
        responseDto.setProjectId(projectId);
        List<CollectionResponse> collections = Collections.singletonList(responseDto);

        when(collectionService.getAllCollectionsForProject(projectId, MOCK_USER_ID_UUID)).thenReturn(collections);

        mockMvc.perform(get("/api/projects/{projectId}/collections", projectId)
                .with(defaultUserJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Test Collection"));
    }
}
