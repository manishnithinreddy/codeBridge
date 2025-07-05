package com.codebridge.security.identity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, unique = true)
    private ERole name;

    @Column(length = 100)
    private String description;

    public enum ERole {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_MODERATOR,
        ROLE_ORGANIZATION_ADMIN,
        ROLE_TEAM_ADMIN
    }
}
