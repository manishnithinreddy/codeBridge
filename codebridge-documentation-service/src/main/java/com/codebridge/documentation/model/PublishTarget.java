package com.codebridge.documentation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a documentation publish target.
 */
@Entity
@Table(name = "publish_targets")
@Data
@NoArgsConstructor
public class PublishTarget {

    /**
     * Enum representing publish target types.
     */
    public enum TargetType {
        /**
         * File system target.
         */
        FILE_SYSTEM,
        
        /**
         * S3 target.
         */
        S3,
        
        /**
         * Git repository target.
         */
        GIT,
        
        /**
         * FTP target.
         */
        FTP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TargetType type;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

