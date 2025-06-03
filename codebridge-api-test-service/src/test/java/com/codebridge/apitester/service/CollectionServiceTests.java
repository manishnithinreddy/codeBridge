package com.codebridge.apitester.service;

import com.codebridge.apitester.model.Collection;
import com.codebridge.apitester.model.Project;
import com.codebridge.apitester.model.enums.SharePermissionLevel;
import com.codebridge.apitester.dto.CollectionRequest;
import com.codebridge.apitester.dto.CollectionResponse;
import com.codebridge.apitester.dto.ProjectResponse;
import com.codebridge.apitester.exception.AccessDeniedException;
import com.codebridge.apitester.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.CollectionRepository; // Corrected package
import com.codebridge.apitest.repository.ProjectRepository;   // Corrected package
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTests {

    @Mock private CollectionRepository collectionRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectSharingService projectSharingService;

    @InjectMocks private CollectionService collectionService;

    private UUID userId;
    private UUID projectId;
    private UUID collectionId;
    private Project project;
    private Collection collectionInProject;
    private Collection standaloneCollection;
    private CollectionRequest collectionRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        collectionId = UUID.randomUUID();

        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        project.setPlatformUserId(userId); // userId owns this project

        collectionInProject = new Collection();
        collectionInProject.setId(collectionId);
        collectionInProject.setName("Project Collection");
        collectionInProject.setProject(project); // Associated with project
        collectionInProject.setUserId(userId); // Creator

        standaloneCollection = new Collection();
        standaloneCollection.setId(UUID.randomUUID());
        standaloneCollection.setName("Standalone Collection");
        standaloneCollection.setUserId(userId); // Owned by userId
        standaloneCollection.setProject(null);

        collectionRequest = new CollectionRequest();
        collectionRequest.setName("New Collection");
        collectionRequest.setProjectId(projectId);
    }

    // --- createCollection ---
    @Test
    void createCollection_userHasCanEditPermission_success() {
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.CAN_EDIT);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));

        CollectionResponse response = collectionService.createCollection(collectionRequest, userId);
        assertNotNull(response);
        assertEquals("New Collection", response.getName());
        assertEquals(projectId, response.getProjectId());
    }

    @Test
    void createCollection_userHasViewOnlyPermission_throwsAccessDenied() {
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.VIEW_ONLY);
        assertThrows(AccessDeniedException.class, () -> collectionService.createCollection(collectionRequest, userId));
    }

    // --- getCollectionByIdForUser ---
    @Test
    void getCollectionByIdForUser_inProject_userHasViewAccess_success() {
        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collectionInProject));
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.VIEW_ONLY);

        CollectionResponse response = collectionService.getCollectionByIdForUser(collectionId, userId);
        assertNotNull(response);
        assertEquals(collectionInProject.getName(), response.getName());
    }

    @Test
    void getCollectionByIdForUser_inProject_userNoProjectAccess_throwsResourceNotFound() {
        UUID otherUserId = UUID.randomUUID();
        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collectionInProject));
        when(projectSharingService.getEffectivePermission(projectId, otherUserId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> collectionService.getCollectionByIdForUser(collectionId, otherUserId));
    }

    @Test
    void getCollectionByIdForUser_standalone_userIsOwner_success() {
        when(collectionRepository.findById(standaloneCollection.getId())).thenReturn(Optional.of(standaloneCollection));
        // No projectSharingService mock needed as it's standalone path
        CollectionResponse response = collectionService.getCollectionByIdForUser(standaloneCollection.getId(), userId);
        assertNotNull(response);
        assertEquals(standaloneCollection.getName(), response.getName());
    }

    @Test
    void getCollectionByIdForUser_standalone_userNotOwner_throwsResourceNotFound() {
        UUID otherUserId = UUID.randomUUID();
        when(collectionRepository.findById(standaloneCollection.getId())).thenReturn(Optional.of(standaloneCollection));
        assertThrows(ResourceNotFoundException.class, () -> collectionService.getCollectionByIdForUser(standaloneCollection.getId(), otherUserId));
    }

    // --- getAllCollectionsForUser ---
    @Test
    void getAllCollectionsForUser_combinesOwnedStandalone_ownedProjectCollections_sharedProjectCollections() {
        // User owns 'standaloneCollection' and 'project' (which contains 'collectionInProject')
        Project sharedProjectEntity = new Project(); sharedProjectEntity.setId(UUID.randomUUID()); sharedProjectEntity.setName("Shared Project");
        ProjectResponse sharedProjectDto = new ProjectResponse(sharedProjectEntity.getId(), "Shared Project", "", UUID.randomUUID(), null, null);
        Collection collectionInSharedProject = new Collection(); collectionInSharedProject.setId(UUID.randomUUID()); collectionInSharedProject.setName("Coll in Shared"); collectionInSharedProject.setProject(sharedProjectEntity);

        when(collectionRepository.findByUserId(userId)).thenReturn(List.of(standaloneCollection, collectionInProject)); // This repo method might return all by userId
        // For the test, let's refine the mocking based on service logic:
        // 1. Standalone owned:
        when(collectionRepository.findByUserId(userId)).thenReturn(List.of(standaloneCollection));
        // 2. Collections in owned projects:
        when(projectRepository.findByPlatformUserId(userId)).thenReturn(List.of(project));
        when(collectionRepository.findByProjectId(project.getId())).thenReturn(List.of(collectionInProject));
        // 3. Collections in shared projects:
        when(projectSharingService.listSharedProjectsForUser(userId)).thenReturn(List.of(sharedProjectDto));
        when(projectSharingService.getEffectivePermission(sharedProjectDto.getId(), userId)).thenReturn(SharePermissionLevel.VIEW_ONLY);
        when(collectionRepository.findByProjectId(sharedProjectDto.getId())).thenReturn(List.of(collectionInSharedProject));

        List<CollectionResponse> result = collectionService.getAllCollectionsForUser(userId);

        assertEquals(3, result.size()); // standalone, collectionInProject, collectionInSharedProject
        assertTrue(result.stream().anyMatch(c -> c.getName().equals("Standalone Collection")));
        assertTrue(result.stream().anyMatch(c -> c.getName().equals("Project Collection")));
        assertTrue(result.stream().anyMatch(c -> c.getName().equals("Coll in Shared")));
    }

    // --- getAllCollectionsForProject ---
     @Test
    void getAllCollectionsForProject_userHasViewAccess_success() {
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.VIEW_ONLY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(collectionRepository.findByProjectId(projectId)).thenReturn(List.of(collectionInProject));

        List<CollectionResponse> result = collectionService.getAllCollectionsForProject(projectId, userId);
        assertEquals(1, result.size());
        assertEquals(collectionInProject.getName(), result.get(0).getName());
    }

    @Test
    void getAllCollectionsForProject_userNoAccess_throwsAccessDenied() {
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(null);
        assertThrows(AccessDeniedException.class, () -> collectionService.getAllCollectionsForProject(projectId, userId));
    }

    // --- updateCollection ---
    @Test
    void updateCollection_inProject_userHasCanEdit_success() {
        collectionRequest.setName("Updated Project Collection");
        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collectionInProject));
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.CAN_EDIT);
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> inv.getArgument(0));

        CollectionResponse response = collectionService.updateCollection(collectionId, collectionRequest, userId);
        assertEquals("Updated Project Collection", response.getName());
    }

    @Test
    void updateCollection_inProject_userHasViewOnly_throwsAccessDenied() {
        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collectionInProject));
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.VIEW_ONLY);
        assertThrows(AccessDeniedException.class, () -> collectionService.updateCollection(collectionId, collectionRequest, userId));
    }

    // --- deleteCollection ---
     @Test
    void deleteCollection_inProject_userHasCanEdit_success() {
        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(collectionInProject));
        when(projectSharingService.getEffectivePermission(projectId, userId)).thenReturn(SharePermissionLevel.CAN_EDIT);
        doNothing().when(collectionRepository).delete(collectionInProject);

        assertDoesNotThrow(() -> collectionService.deleteCollection(collectionId, userId));
        verify(collectionRepository).delete(collectionInProject);
    }
}
