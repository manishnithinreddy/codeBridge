package com.codebridge.teams.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter for extracting roles and team information from JWT tokens.
 * Supports both standard roles and team-specific roles.
 */
public class TeamRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String TEAMS_CLAIM = "teams";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Converts JWT claims into granted authorities.
     *
     * @param jwt the JWT token
     * @return a collection of granted authorities
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Extract standard roles
        authorities.addAll(extractRoles(jwt));
        
        // Extract team-specific roles
        authorities.addAll(extractTeamRoles(jwt));
        
        return authorities;
    }

    /**
     * Extracts standard roles from the JWT.
     *
     * @param jwt the JWT token
     * @return a collection of role-based authorities
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        if (jwt.hasClaim(ROLES_CLAIM)) {
            List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
            if (roles != null) {
                authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                    .collect(Collectors.toList()));
            }
        }
        
        return authorities;
    }

    /**
     * Extracts team-specific roles from the JWT.
     *
     * @param jwt the JWT token
     * @return a collection of team-specific authorities
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractTeamRoles(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        if (jwt.hasClaim(TEAMS_CLAIM)) {
            try {
                Map<String, Object> teams = jwt.getClaimAsMap(TEAMS_CLAIM);
                if (teams != null) {
                    teams.forEach((teamId, rolesObj) -> {
                        if (rolesObj instanceof List) {
                            List<String> roles = (List<String>) rolesObj;
                            roles.forEach(role -> {
                                // Format: TEAM_{TEAM_ID}_{ROLE}
                                String authority = "TEAM_" + teamId + "_" + role.toUpperCase();
                                authorities.add(new SimpleGrantedAuthority(authority));
                            });
                        }
                    });
                }
            } catch (Exception e) {
                // Log error but continue with other authorities
                System.err.println("Error extracting team roles: " + e.getMessage());
            }
        }
        
        return authorities;
    }
}

