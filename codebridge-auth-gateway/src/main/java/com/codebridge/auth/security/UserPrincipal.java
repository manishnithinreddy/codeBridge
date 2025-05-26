package com.codebridge.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Custom user principal for authentication.
 * Extends Spring Security's User class with additional properties.
 */
public class UserPrincipal extends User {

    private final String id;
    private String teamId;

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
}

