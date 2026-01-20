package com.codebridge.gitlab.git.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for representing a shared stash in API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStashDTO {

    /**
     * The ID of the shared stash.
     */
    private Long id;

    /**
     * The Git hash of the stash commit.
     */
    private String stashHash;

    /**
     * The ID of the repository that this stash belongs to.
     */
    private Long repositoryId;

    /**
     * The name of the repository that this stash belongs to.
     */
    private String repositoryName;

    /**
     * The user who shared this stash.
     */
    private String sharedBy;

    /**
     * The timestamp when this stash was shared.
     */
    private LocalDateTime sharedAt;

    /**
     * A description of the stash provided by the user.
     */
    private String description;

    /**
     * The branch that the stash was created from.
     */
    private String branch;
}

