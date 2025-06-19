package com.codebridge.gitlab.git.service.impl;

import com.codebridge.gitlab.git.model.Repository;
import com.codebridge.gitlab.git.model.SharedStash;
import com.codebridge.gitlab.git.repository.RepositoryRepository;
import com.codebridge.gitlab.git.repository.SharedStashRepository;
import com.codebridge.gitlab.git.service.SharedStashService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of the SharedStashService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SharedStashServiceImpl implements SharedStashService {

    private final SharedStashRepository sharedStashRepository;
    private final RepositoryRepository repositoryRepository;
    private final GitCommandExecutor gitCommandExecutor;

    @Override
    @Transactional
    public SharedStash registerSharedStash(String stashHash, Long repositoryId, String description, String sharedBy, String branch) {
        log.debug("Registering shared stash with hash {} for repository {}", stashHash, repositoryId);
        
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found with ID: " + repositoryId));
        
        // Check if a stash with this hash already exists for this repository
        if (sharedStashRepository.existsByStashHashAndRepository(stashHash, repository)) {
            log.debug("Stash with hash {} already exists for repository {}", stashHash, repositoryId);
            return sharedStashRepository.findByStashHashAndRepository(stashHash, repository);
        }
        
        SharedStash sharedStash = SharedStash.builder()
                .stashHash(stashHash)
                .repository(repository)
                .sharedBy(sharedBy)
                .sharedAt(LocalDateTime.now())
                .description(description)
                .branch(branch)
                .build();
        
        return sharedStashRepository.save(sharedStash);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharedStash> getSharedStashes(Long repositoryId) {
        log.debug("Getting shared stashes for repository {}", repositoryId);
        
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found with ID: " + repositoryId));
        
        return sharedStashRepository.findByRepositoryOrderBySharedAtDesc(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedStash getSharedStash(Long id) {
        log.debug("Getting shared stash with ID {}", id);
        
        return sharedStashRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shared stash not found with ID: " + id));
    }

    @Override
    @Transactional
    public void deleteSharedStash(Long id) {
        log.debug("Deleting shared stash with ID {}", id);
        
        SharedStash sharedStash = sharedStashRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shared stash not found with ID: " + id));
        
        sharedStashRepository.delete(sharedStash);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStashHashAndRepository(String stashHash, Long repositoryId) {
        log.debug("Checking if stash with hash {} exists for repository {}", stashHash, repositoryId);
        
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found with ID: " + repositoryId));
        
        return sharedStashRepository.existsByStashHashAndRepository(stashHash, repository);
    }
}

