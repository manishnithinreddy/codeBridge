package com.codebridge.gitlab.git.repository;

import com.codebridge.gitlab.git.model.Repository;
import com.codebridge.gitlab.git.model.SharedStash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for accessing SharedStash entities.
 */
@org.springframework.stereotype.Repository
public interface SharedStashRepository extends JpaRepository<SharedStash, UUID> {

    /**
     * Find all shared stashes for a specific repository.
     *
     * @param repository The repository to find shared stashes for
     * @return A list of shared stashes for the repository
     */
    List<SharedStash> findByRepositoryOrderBySharedAtDesc(Repository repository);

    /**
     * Find a shared stash by its hash and repository.
     *
     * @param stashHash The hash of the stash
     * @param repository The repository the stash belongs to
     * @return The shared stash, if found
     */
    SharedStash findByStashHashAndRepository(String stashHash, Repository repository);

    /**
     * Check if a stash with the given hash exists in the repository.
     *
     * @param stashHash The hash of the stash
     * @param repository The repository to check
     * @return True if the stash exists, false otherwise
     */
    boolean existsByStashHashAndRepository(String stashHash, Repository repository);
}
