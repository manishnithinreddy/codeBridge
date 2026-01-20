package com.codebridge.gitlab.git.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a shared Git stash.
 * This stores metadata about a stash that has been shared by a user.
 */
@Entity
@Table(name = "shared_stashes")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStash extends BaseEntity {

    /**
     * The Git hash of the stash commit.
     */
    @Column(name = "stash_hash", nullable = false)
    private String stashHash;

    /**
     * The repository that this stash belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    /**
     * The user who shared this stash.
     */
    @Column(name = "shared_by", nullable = false)
    private String sharedBy;

    /**
     * The timestamp when this stash was shared.
     */
    @Column(name = "shared_at", nullable = false)
    private LocalDateTime sharedAt;

    /**
     * A description of the stash provided by the user.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * The branch that the stash was created from.
     */
    @Column(name = "branch")
    private String branch;
}

