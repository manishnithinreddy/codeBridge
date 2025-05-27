package com.codebridge.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

/**
 * Custom user principal for authentication.
 * Extends Spring Security's User class with additional properties.
 */
public class UserPrincipal extends User {

    private final String id;
    private String teamId;
    private String email;
    private String name;
    private List<String> permissions;

    /**
     * Creates a new user principal.
     *
     * @param id the user ID
     * @param username the username
     * @param password the password
     * @param authorities the granted authorities
     */
    public UserPrincipal(String id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the team ID.
     *
     * @return the team ID
     */
    public String getTeamId() {
        return teamId;
    }

    /**
     * Sets the team ID.
     *
     * @param teamId the team ID
     */
    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    /**
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     *
     * @param email the email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Sets the permissions.
     *
     * @param permissions the permissions
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}

