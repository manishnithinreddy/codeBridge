package com.codebridge.gitlab.git.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting to share a stash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareStashRequest {

    /**
     * The Git hash of the stash commit.
     */
    @NotBlank(message = "Stash hash is required")
    private String stashHash;

    /**
     * The ID of the repository that this stash belongs to.
     */
    @NotNull(message = "Repository ID is required")
    private Long repositoryId;

    /**
     * A description of the stash provided by the user.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * The branch that the stash was created from.
     */
    private String branch;
}

