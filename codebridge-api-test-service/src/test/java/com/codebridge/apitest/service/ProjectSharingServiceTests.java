package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Project;
import com.codebridge.apitest.model.ShareGrant;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.dto.ShareGrantRequest;
import com.codebridge.apitest.dto.ShareGrantResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.ProjectRepository; // Corrected
import com.codebridge.apitest.repository.ShareGrantRepository; // Corrected
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSharingServiceTests {

    @Mock private ShareGrantRepository shareGrantRepository;
    @Mock private ProjectRepository projectRepository;
    // ProjectService not mocked as its mapProjectToProjectResponse is not used directly,
    // a local one is used in ProjectSharingService.

    @InjectMocks private ProjectSharingService projectSharingService;

    private UUID ownerId;
    private UUID granteeId;
    private UUID projectId;
    private Project project;
    private ShareGrantRequest shareGrantRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        granteeId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
        project.setName("Shared Project");
        project.setPlatformUserId(ownerId); // ownerId owns this project

        shareGrantRequest = new ShareGrantRequest();
        shareGrantRequest.setGranteeUserId(granteeId);
        shareGrantRequest.setPermissionLevel(com.codebridge.apitest.model.enums.SharePermissionLevel.CAN_VIEW); // Fully qualified
    }

    // --- grantProjectAccess ---
    @Test
    void grantProjectAccess_byOwner_newGrant_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, granteeId)).thenReturn(Optional.empty());
        when(shareGrantRepository.save(any(ShareGrant.class))).thenAnswer(inv -> {
            ShareGrant sg = inv.getArgument(0);
            sg.setId(UUID.randomUUID()); // Simulate save
            return sg;
        });

        ShareGrantResponse response = projectSharingService.grantProjectAccess(projectId, shareGrantRequest, ownerId);

        assertNotNull(response);
        assertEquals(granteeId, response.getGranteeUserId());
        assertEquals(com.codebridge.apitest.model.enums.SharePermissionLevel.CAN_VIEW, response.getPermissionLevel()); // Fully qualified
        verify(shareGrantRepository).save(any(ShareGrant.class));
    }

    @Test
    void grantProjectAccess_byOwner_updateGrant_success() {
        ShareGrant existingGrant = new ShareGrant(project, granteeId, com.codebridge.apitest.model.enums.SharePermissionLevel.VIEW_ONLY, ownerId); // Fully qualified
        existingGrant.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, granteeId)).thenReturn(Optional.of(existingGrant));
        when(shareGrantRepository.save(any(ShareGrant.class))).thenAnswer(inv -> inv.getArgument(0));

        shareGrantRequest.setPermissionLevel(com.codebridge.apitest.model.enums.SharePermissionLevel.CAN_EDIT); // Fully qualified
        ShareGrantResponse response = projectSharingService.grantProjectAccess(projectId, shareGrantRequest, ownerId);

        assertEquals(com.codebridge.apitest.model.enums.SharePermissionLevel.CAN_EDIT, response.getPermissionLevel()); // Fully qualified
        verify(shareGrantRepository).save(existingGrant); // Ensure it updated existing
    }


    @Test
    void grantProjectAccess_byNonOwner_throwsAccessDenied() {
        UUID nonOwnerId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project)); // project owned by ownerId
        assertThrows(AccessDeniedException.class, () -> projectSharingService.grantProjectAccess(projectId, shareGrantRequest, nonOwnerId));
    }

    @Test
    void grantProjectAccess_toSelf_throwsIllegalArgument() {
        shareGrantRequest.setGranteeUserId(ownerId); // Granting to self
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        assertThrows(IllegalArgumentException.class, () -> projectSharingService.grantProjectAccess(projectId, shareGrantRequest, ownerId));
    }

    // --- revokeProjectAccess ---
    @Test
    void revokeProjectAccess_byOwner_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doNothing().when(shareGrantRepository).deleteByProjectIdAndGranteeUserId(projectId, granteeId);
        assertDoesNotThrow(() -> projectSharingService.revokeProjectAccess(projectId, granteeId, ownerId));
        verify(shareGrantRepository).deleteByProjectIdAndGranteeUserId(projectId, granteeId);
    }

    @Test
    void revokeProjectAccess_byNonOwner_throwsAccessDenied() {
        UUID nonOwnerId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        assertThrows(AccessDeniedException.class, () -> projectSharingService.revokeProjectAccess(projectId, granteeId, nonOwnerId));
    }

    // --- getEffectivePermission ---
    @Test
    void getEffectivePermission_forOwner_returnsCanEdit() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project)); // project owned by ownerId
        assertEquals(com.codebridge.apitest.model.enums.SharePermissionLevel.CAN_EDIT, projectSharingService.getEffectivePermission(projectId, ownerId)); // Fully qualified
    }

    @Test
    void getEffectivePermission_forGrantee_returnsGrantLevel() {
        ShareGrant grant = new ShareGrant(project, granteeId, com.codebridge.apitest.model.enums.SharePermissionLevel.VIEW_ONLY, ownerId); // Fully qualified
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, granteeId)).thenReturn(Optional.of(grant));
        assertEquals(com.codebridge.apitest.model.enums.SharePermissionLevel.VIEW_ONLY, projectSharingService.getEffectivePermission(projectId, granteeId)); // Fully qualified
    }

    @Test
    void getEffectivePermission_forNonGrantee_returnsNull() {
        UUID otherUserId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(shareGrantRepository.findByProjectIdAndGranteeUserId(projectId, otherUserId)).thenReturn(Optional.empty());
        assertNull(projectSharingService.getEffectivePermission(projectId, otherUserId));
    }

    // --- deleteAllGrantsForProject ---
    @Test
    void deleteAllGrantsForProject_callsRepository() {
        doNothing().when(shareGrantRepository).deleteByProjectId(projectId);
        projectSharingService.deleteAllGrantsForProject(projectId);
        verify(shareGrantRepository).deleteByProjectId(projectId);
    }
}
