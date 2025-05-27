package com.codebridge.usermanagement.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Entity representing a role in the system.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(max = 50)
    private String name;

    @Column
    @Size(max = 255)
    private String description;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

