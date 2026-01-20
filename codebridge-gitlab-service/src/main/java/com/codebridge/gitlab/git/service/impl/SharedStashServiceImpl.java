package com.codebridge.gitlab.git.service.impl;

import com.codebridge.gitlab.git.model.Repository;
import com.codebridge.gitlab.git.model.SharedStash;
import com.codebridge.gitlab.git.repository.RepositoryRepository;
import com.codebridge.gitlab.git.repository.SharedStashRepository;
import com.codebridge.gitlab.git.service.SharedStashService;
import com.codebridge.gitlab.git.service.impl.GitCommandExecutor;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the SharedStashService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SharedStashServiceImpl implements SharedStashService {

    private static final Logger log = LoggerFactory.getLogger(SharedStashServiceImpl.class);
    
    private final SharedStashRepository sharedStashRepository;
    private final RepositoryRepository repositoryRepository;
    private final GitCommandExecutor gitCommandExecutor;

    @Override
    @Transactional
    public SharedStash registerSharedStash(String stashHash, UUID repositoryId, String description, String sharedBy, String branch) {
        log.debug("Registering shared stash with hash {} for repository {}", stashHash, repositoryId);
        
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found with ID: " + repositoryId));
        
        // Check if a stash with this hash already exists for this repository
        if (sharedStashRepository.existsByStashHashAndRepository(stashHash, repository)) {
            log.debug("Stash with hash {} already exists for repository {}", stashHash, repositoryId);
            return sharedStashRepository.findByStashHashAndRepository(stashHash, repository);
        }
        
        SharedStash sharedStash = new SharedStash();
        sharedStash.setStashHash(stashHash);
        sharedStash.setRepository(repository);
        sharedStash.setSharedBy(sharedBy);
        sharedStash.setSharedAt(LocalDateTime.now());
        sharedStash.setDescription(description);
        sharedStash.setBranch(branch);
        
        return sharedStashRepository.save(sharedStash);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharedStash> getSharedStashes(UUID repositoryId) {
        log.debug("Getting shared stashes for repository {}", repositoryId);
        
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found with ID: " + repositoryId));
        
        return sharedStashRepository.findByRepositoryOrderBySharedAtDesc(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedStash getSharedStash(UUID id) {
        log.debug("Getting shared stash with ID {}", id);
        
        return sharedStashRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shared stash not found with ID: " + id));
    }

    @Override
    @Transactional
    public void deleteSharedStash(UUID id) {
        log.debug("Deleting shared stash with ID {}", id);
        
        SharedStash sharedStash = sharedStashRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shared stash not found with ID: " + id));
        
        sharedStashRepository.delete(sharedStash);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStashHashAndRepository(String stashHash, UUID repositoryId) {
        log.debug("Checking if stash with hash {} exists for repository {}", stashHash, repositoryId);
        
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found with ID: " + repositoryId));
        
        return sharedStashRepository.existsByStashHashAndRepository(stashHash, repository);
    }
}
