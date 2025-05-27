package com.codebridge.security.service;

import com.codebridge.core.security.UserPrincipal;
import com.codebridge.security.model.Role;
import com.codebridge.security.model.User;
import com.codebridge.security.model.UserRole;
import com.codebridge.security.repository.UserRepository;
import com.codebridge.security.repository.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom implementation of UserDetailsService.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        return createUserPrincipal(user);
    }

    /**
     * Loads a user by ID.
     *
     * @param id the user ID
     * @return the user details
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(String id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        
        return createUserPrincipal(user);
    }

    /**
     * Creates a UserPrincipal from a User entity.
     *
     * @param user the user entity
     * @return the user principal
     */
    private UserPrincipal createUserPrincipal(User user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        
        List<GrantedAuthority> authorities = userRoles.stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()))
                .collect(Collectors.toList());
        
        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities
        );
        
        userPrincipal.setEmail(user.getEmail());
        userPrincipal.setName(user.getName());
        
        if (user.getTeamId() != null) {
            userPrincipal.setTeamId(user.getTeamId().toString());
        }
        
        // Add permissions if available
        // This would typically come from a separate permissions table
        // For now, we'll leave it empty
        
        return userPrincipal;
    }
}

