package com.codebridge.gitlab.git.service.impl;

import com.codebridge.gitlab.git.model.Repository;
import com.codebridge.gitlab.git.model.SharedStash;
import com.codebridge.gitlab.git.repository.RepositoryRepository;
import com.codebridge.gitlab.git.repository.SharedStashRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedStashServiceImplTest {

    @Mock
    private SharedStashRepository sharedStashRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private GitCommandExecutor gitCommandExecutor;

    @InjectMocks
    private SharedStashServiceImpl sharedStashService;

    private Repository repository;
    private SharedStash sharedStash;
    private final UUID repositoryId = UUID.randomUUID();
    private final UUID sharedStashId = UUID.randomUUID();
    private final String stashHash = "abcd1234";
    private final String description = "Test stash";
    private final String sharedBy = "testuser";
    private final String branch = "main";

    @BeforeEach
    void setUp() {
        repository = new Repository();
        repository.setId(repositoryId);
        repository.setName("test-repo");

        sharedStash = SharedStash.builder()
                .stashHash(stashHash)
                .repository(repository)
                .sharedBy(sharedBy)
                .sharedAt(LocalDateTime.now())
                .description(description)
                .branch(branch)
                .build();
        sharedStash.setId(sharedStashId);
    }

    @Test
    void registerSharedStash_ShouldCreateNewStash_WhenStashDoesNotExist() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository));
        when(sharedStashRepository.existsByStashHashAndRepository(stashHash, repository)).thenReturn(false);
        when(sharedStashRepository.save(any(SharedStash.class))).thenReturn(sharedStash);

        // Act
        SharedStash result = sharedStashService.registerSharedStash(stashHash, repositoryId, description, sharedBy, branch);

        // Assert
        assertNotNull(result);
        assertEquals(stashHash, result.getStashHash());
        assertEquals(repository, result.getRepository());
        assertEquals(description, result.getDescription());
        assertEquals(sharedBy, result.getSharedBy());
        assertEquals(branch, result.getBranch());

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository).existsByStashHashAndRepository(stashHash, repository);
        verify(sharedStashRepository).save(any(SharedStash.class));
    }

    @Test
    void registerSharedStash_ShouldReturnExistingStash_WhenStashExists() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository));
        when(sharedStashRepository.existsByStashHashAndRepository(stashHash, repository)).thenReturn(true);
        when(sharedStashRepository.findByStashHashAndRepository(stashHash, repository)).thenReturn(sharedStash);

        // Act
        SharedStash result = sharedStashService.registerSharedStash(stashHash, repositoryId, description, sharedBy, branch);

        // Assert
        assertNotNull(result);
        assertEquals(sharedStash, result);

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository).existsByStashHashAndRepository(stashHash, repository);
        verify(sharedStashRepository).findByStashHashAndRepository(stashHash, repository);
        verify(sharedStashRepository, never()).save(any(SharedStash.class));
    }

    @Test
    void registerSharedStash_ShouldThrowException_WhenRepositoryNotFound() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
                sharedStashService.registerSharedStash(stashHash, repositoryId, description, sharedBy, branch));

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository, never()).existsByStashHashAndRepository(anyString(), any(Repository.class));
        verify(sharedStashRepository, never()).save(any(SharedStash.class));
    }

    @Test
    void getSharedStashes_ShouldReturnStashes() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository));
        when(sharedStashRepository.findByRepositoryOrderBySharedAtDesc(repository)).thenReturn(List.of(sharedStash));

        // Act
        List<SharedStash> result = sharedStashService.getSharedStashes(repositoryId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sharedStash, result.get(0));

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository).findByRepositoryOrderBySharedAtDesc(repository);
    }

    @Test
    void getSharedStashes_ShouldThrowException_WhenRepositoryNotFound() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
                sharedStashService.getSharedStashes(repositoryId));

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository, never()).findByRepositoryOrderBySharedAtDesc(any(Repository.class));
    }

    @Test
    void getSharedStash_ShouldReturnStash() {
        // Arrange
        UUID stashId = UUID.randomUUID();
        when(sharedStashRepository.findById(stashId)).thenReturn(Optional.of(sharedStash));

        // Act
        SharedStash result = sharedStashService.getSharedStash(stashId);

        // Assert
        assertNotNull(result);
        assertEquals(sharedStash, result);

        verify(sharedStashRepository).findById(stashId);
    }

    @Test
    void getSharedStash_ShouldThrowException_WhenStashNotFound() {
        // Arrange
        UUID stashId = UUID.randomUUID();
        when(sharedStashRepository.findById(stashId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
                sharedStashService.getSharedStash(stashId));

        verify(sharedStashRepository).findById(stashId);
    }

    @Test
    void deleteSharedStash_ShouldDeleteStash() {
        // Arrange
        UUID stashId = UUID.randomUUID();
        when(sharedStashRepository.findById(stashId)).thenReturn(Optional.of(sharedStash));

        // Act
        sharedStashService.deleteSharedStash(stashId);

        // Assert
        verify(sharedStashRepository).findById(stashId);
        verify(sharedStashRepository).delete(sharedStash);
    }

    @Test
    void deleteSharedStash_ShouldThrowException_WhenStashNotFound() {
        // Arrange
        UUID stashId = UUID.randomUUID();
        when(sharedStashRepository.findById(stashId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
                sharedStashService.deleteSharedStash(stashId));

        verify(sharedStashRepository).findById(stashId);
        verify(sharedStashRepository, never()).delete(any(SharedStash.class));
    }

    @Test
    void existsByStashHashAndRepository_ShouldReturnTrue_WhenStashExists() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository));
        when(sharedStashRepository.existsByStashHashAndRepository(stashHash, repository)).thenReturn(true);

        // Act
        boolean result = sharedStashService.existsByStashHashAndRepository(stashHash, repositoryId);

        // Assert
        assertTrue(result);

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository).existsByStashHashAndRepository(stashHash, repository);
    }

    @Test
    void existsByStashHashAndRepository_ShouldReturnFalse_WhenStashDoesNotExist() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository));
        when(sharedStashRepository.existsByStashHashAndRepository(stashHash, repository)).thenReturn(false);

        // Act
        boolean result = sharedStashService.existsByStashHashAndRepository(stashHash, repositoryId);

        // Assert
        assertFalse(result);

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository).existsByStashHashAndRepository(stashHash, repository);
    }

    @Test
    void existsByStashHashAndRepository_ShouldThrowException_WhenRepositoryNotFound() {
        // Arrange
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> 
                sharedStashService.existsByStashHashAndRepository(stashHash, repositoryId));

        verify(repositoryRepository).findById(repositoryId);
        verify(sharedStashRepository, never()).existsByStashHashAndRepository(anyString(), any(Repository.class));
    }
}
