package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Project;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.dto.ProjectRequest;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.DuplicateResourceException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository; // Corrected package
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectSharingService projectSharingService;

    @InjectMocks private ProjectService projectService;

    private UUID userId;
    private UUID projectId;
    private Project project;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        project.setPlatformUserId(userId); // userId is the owner
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        projectRequest = new ProjectRequest();
        projectRequest.setName("New Project Name");
        projectRequest.setDescription("New Description");
    }

    // --- createProject ---
    @Test
    void createProject_success() {
        when(projectRepository.existsByNameAndPlatformUserId(projectRequest.getName(), userId)).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.createProject(projectRequest, userId);

        assertNotNull(response);
        assertEquals(projectRequest.getName(), response.getName());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_duplicateName_throwsDuplicateResourceException() {
        when(projectRepository.existsByNameAndPlatformUserId(projectRequest.getName(), userId)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> projectService.createProject(projectRequest, userId));
    }

    // --- getProjectByIdForUser ---
    @Test
    void getProjectByIdForUser_isOwner_returnsProject() {
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.CAN_EDIT); // Owner gets CAN_EDIT
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        ProjectResponse response = projectService.getProjectByIdForUser(projectId, userId);
        assertNotNull(response);
        assertEquals(project.getName(), response.getName());
    }

    @Test
    void getProjectByIdForUser_hasShareGrant_returnsProject() {
        UUID granteeId = UUID.randomUUID();
        when(projectSharingService.getEffectivePermission(projectId, granteeId)).thenReturn(SharePermissionLevel.VIEW_ONLY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project)); // project is owned by 'userId'
        ProjectResponse response = projectService.getProjectByIdForUser(projectId, granteeId);
        assertNotNull(response);
    }

    @Test
    void getProjectByIdForUser_noAccess_throwsResourceNotFound() {
        UUID otherUserId = UUID.randomUUID();
        when(projectSharingService.getEffectivePermission(projectId, otherUserId)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectByIdForUser(projectId, otherUserId));
    }

    // --- listProjectsForUser ---
    @Test
    void listProjectsForUser_combinesOwnedAndShared() {
        Project ownedProject = new Project(); ownedProject.setId(UUID.randomUUID()); ownedProject.setName("Owned"); ownedProject.setPlatformUserId(userId);
        ProjectResponse sharedProjectResponse = new ProjectResponse(UUID.randomUUID(), "Shared", "", UUID.randomUUID(), null, null);

        when(projectRepository.findByPlatformUserId(userId)).thenReturn(Collections.singletonList(ownedProject));
        when(projectSharingService.listSharedProjectsForUser(userId)).thenReturn(Collections.singletonList(sharedProjectResponse));

        List<ProjectResponse> result = projectService.listProjectsForUser(userId);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("Owned")));
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("Shared")));
    }

    // --- updateProject ---
    @Test
    void updateProject_isOwner_success() {
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.CAN_EDIT);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectRequest.setName("Updated Name");
        ProjectResponse response = projectService.updateProject(projectId, projectRequest, userId);
        assertEquals("Updated Name", response.getName());
    }

    @Test
    void updateProject_hasCanEditShare_success() {
        UUID editorId = UUID.randomUUID();
        project.setPlatformUserId(userId); // Original owner is different
        when(projectSharingService.getEffectivePermission(projectId, editorId)).thenReturn(SharePermissionLevel.CAN_EDIT);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectRequest.setName("Updated By Editor");
        ProjectResponse response = projectService.updateProject(projectId, projectRequest, editorId);
        assertEquals("Updated By Editor", response.getName());
    }

    @Test
    void updateProject_hasViewOnlyShare_throwsAccessDenied() {
        UUID viewerId = UUID.randomUUID();
        when(projectSharingService.getEffectivePermission(projectId, viewerId)).thenReturn(SharePermissionLevel.VIEW_ONLY);
        assertThrows(AccessDeniedException.class, () -> projectService.updateProject(projectId, projectRequest, viewerId));
    }

    // --- deleteProject ---
    @Test
    void deleteProject_isOwner_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project)); // project owned by userId
        doNothing().when(projectSharingService).deleteAllGrantsForProject(projectId);
        doNothing().when(projectRepository).delete(project);

        assertDoesNotThrow(() -> projectService.deleteProject(projectId, userId));
        verify(projectSharingService).deleteAllGrantsForProject(projectId);
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_notOwner_throwsAccessDenied() {
        UUID notOwnerId = UUID.randomUUID();
        project.setPlatformUserId(userId); // Original owner
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        // No need to mock projectSharingService as ownership check is first

        assertThrows(AccessDeniedException.class, () -> projectService.deleteProject(projectId, notOwnerId));
        verify(projectSharingService, never()).deleteAllGrantsForProject(any());
        verify(projectRepository, never()).delete(any());
    }
}
