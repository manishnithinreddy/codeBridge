package com.codebridge.security.identity.model;

import com.codebridge.security.auth.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(unique = true)
    private String name;

    @Size(max = 255)
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @ManyToMany(mappedBy = "organizations")
    private Set<User> users = new HashSet<>();

    @Column(name = "active")
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Manual builder method
    public static OrganizationBuilder builder() {
        return new OrganizationBuilder();
    }

    // Manual builder class
    public static class OrganizationBuilder {
        private Long id;
        private String name;
        private String description;
        private String logoUrl;
        private String websiteUrl;
        private Set<User> users = new HashSet<>();
        private boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrganizationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrganizationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public OrganizationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OrganizationBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public OrganizationBuilder websiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
            return this;
        }

        public OrganizationBuilder users(Set<User> users) {
            this.users = users;
            return this;
        }

        public OrganizationBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public Organization build() {
            Organization organization = new Organization();
            organization.id = this.id;
            organization.name = this.name;
            organization.description = this.description;
            organization.logoUrl = this.logoUrl;
            organization.websiteUrl = this.websiteUrl;
            organization.users = this.users;
            organization.active = this.active;
            organization.createdAt = this.createdAt;
            organization.updatedAt = this.updatedAt;
            return organization;
        }
    }

    // Manual getter for id field
    public Long getId() {
        return id;
    }

    // Manual getter for name field
    public String getName() {
        return name;
    }

    // Manual setter for name field
    public void setName(String name) {
        this.name = name;
    }

    // Manual setter for description field
    public void setDescription(String description) {
        this.description = description;
    }

    // Manual setter for logoUrl field
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    // Manual setter for websiteUrl field
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    // Manual getter for users field
    public Set<User> getUsers() {
        return users;
    }

    // Manual getter for description field
    public String getDescription() {
        return description;
    }

    // Manual getter for logoUrl field
    public String getLogoUrl() {
        return logoUrl;
    }

    // Manual getter for websiteUrl field
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    // Manual getter for active field
    public boolean isActive() {
        return active;
    }

    // Manual getter for createdAt field
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Manual getter for updatedAt field
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
