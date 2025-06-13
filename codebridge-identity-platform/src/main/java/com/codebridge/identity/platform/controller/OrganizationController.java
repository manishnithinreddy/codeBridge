package com.codebridge.identity.platform.controller;

import com.codebridge.identity.platform.dto.OrganizationDto;
import com.codebridge.identity.platform.dto.OrganizationRequest;
import com.codebridge.identity.platform.dto.UserDto;
import com.codebridge.identity.platform.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing organization operations.
 * Provides endpoints for organization CRUD operations and user management.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Get all organizations.
     *
     * @return List of organizations
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    /**
     * Get organization by ID.
     *
     * @param id Organization ID
     * @return Organization details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @organizationSecurity.isMember(#id)")
    public ResponseEntity<OrganizationDto> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    /**
     * Create a new organization.
     *
     * @param request Organization creation request
     * @return Created organization details
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<OrganizationDto> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.createOrganization(request));
    }

    /**
     * Update an organization.
     *
     * @param id Organization ID
     * @param request Organization update request
     * @return Updated organization details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @organizationSecurity.isAdmin(#id)")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, request));
    }

    /**
     * Delete an organization.
     *
     * @param id Organization ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @organizationSecurity.isAdmin(#id)")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all users in an organization.
     *
     * @param id Organization ID
     * @return List of users
     */
    @GetMapping("/{id}/users")
    @PreAuthorize("hasRole('ADMIN') or @organizationSecurity.isMember(#id)")
    public ResponseEntity<List<UserDto>> getOrganizationUsers(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganizationUsers(id));
    }

    /**
     * Add a user to an organization.
     *
     * @param id Organization ID
     * @param userId User ID
     * @return Success response
     */
    @PostMapping("/{id}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @organizationSecurity.isAdmin(#id)")
    public ResponseEntity<?> addUserToOrganization(@PathVariable Long id, @PathVariable Long userId) {
        organizationService.addUserToOrganization(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Remove a user from an organization.
     *
     * @param id Organization ID
     * @param userId User ID
     * @return Success response
     */
    @DeleteMapping("/{id}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @organizationSecurity.isAdmin(#id)")
    public ResponseEntity<?> removeUserFromOrganization(@PathVariable Long id, @PathVariable Long userId) {
        organizationService.removeUserFromOrganization(id, userId);
        return ResponseEntity.ok().build();
    }
}

