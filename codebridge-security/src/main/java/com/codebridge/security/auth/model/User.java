package com.codebridge.security.auth.model;

import com.codebridge.security.rbac.model.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User entity for authentication and authorization.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "account_non_expired")
    private boolean accountNonExpired = true;

    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired = true;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "lockout_time")
    private LocalDateTime lockoutTime;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @ElementCollection
    @CollectionTable(name = "password_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "password")
    private List<String> passwordHistory = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add role-based authorities
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            
            // Add permission-based authorities
            authorities.addAll(role.getPermissions().stream()
                    .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                    .collect(Collectors.toSet()));
        }
        
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    // Manual getter for roles field
    public Set<Role> getRoles() {
        return roles;
    }

    // Manual getter for id field
    public Long getId() {
        return id;
    }

    // Manual getter for failedAttempts field
    public int getFailedAttempts() {
        return failedAttempts;
    }

    // Manual getter for mfaEnabled field
    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    // Manual getter for email field
    public String getEmail() {
        return email;
    }

    // Manual getter for mfaSecret field
    public String getMfaSecret() {
        return mfaSecret;
    }

    // Manual setter for mfaSecret field
    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    // Manual setter for mfaEnabled field
    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    // Manual getter for createdAt field
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Manual getter for updatedAt field
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Manual getter for firstName field
    public String getFirstName() {
        return firstName;
    }

    // Manual getter for lastName field
    public String getLastName() {
        return lastName;
    }

    // Manual setter for firstName field
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // Manual setter for lastName field
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // Manual setter for email field
    public void setEmail(String email) {
        this.email = email;
    }
}
