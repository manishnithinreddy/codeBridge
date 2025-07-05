package com.codebridge.gitlab.git.service;

import com.codebridge.gitlab.git.model.SharedStash;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for operations related to shared stashes.
 */
public interface SharedStashService {

    /**
     * Register a new shared stash.
     *
     * @param stashHash The Git hash of the stash commit
     * @param repositoryId The ID of the repository that this stash belongs to
     * @param description A description of the stash
     * @param sharedBy The user who is sharing the stash
     * @param branch The branch that the stash was created from (optional)
     * @return The created SharedStash entity
     */
    SharedStash registerSharedStash(String stashHash, UUID repositoryId, String description, String sharedBy, String branch);

    /**
     * Get all shared stashes for a specific repository.
     *
     * @param repositoryId The ID of the repository to get shared stashes for
     * @return A list of shared stashes for the repository
     */
    List<SharedStash> getSharedStashes(UUID repositoryId);

    /**
     * Get a specific shared stash by its ID.
     *
     * @param id The ID of the shared stash
     * @return The shared stash, if found
     */
    SharedStash getSharedStash(UUID id);

    /**
     * Delete a shared stash.
     *
     * @param id The ID of the shared stash to delete
     */
    void deleteSharedStash(UUID id);

    /**
     * Check if a stash with the given hash exists in the repository.
     *
     * @param stashHash The hash of the stash
     * @param repositoryId The ID of the repository to check
     * @return True if the stash exists, false otherwise
     */
    boolean existsByStashHashAndRepository(String stashHash, UUID repositoryId);
}
