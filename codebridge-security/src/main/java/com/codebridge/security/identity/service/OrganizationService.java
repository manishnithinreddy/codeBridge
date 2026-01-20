package com.codebridge.security.identity.service;

import com.codebridge.security.identity.dto.OrganizationDto;
import com.codebridge.security.identity.dto.OrganizationRequest;
import com.codebridge.security.identity.dto.UserDto;
import com.codebridge.security.identity.model.Organization;
import com.codebridge.security.identity.model.User;
import com.codebridge.security.identity.repository.OrganizationRepository;
import com.codebridge.security.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    /**
     * Get all organizations.
     *
     * @return List of organization DTOs
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getAllOrganizations() {
        log.debug("Fetching all organizations");
        return organizationRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get organization by ID.
     *
     * @param id Organization ID
     * @return Organization DTO
     * @throws RuntimeException if organization not found
     */
    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationById(Long id) {
        log.debug("Fetching organization with id: {}", id);
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));
        return convertToDto(organization);
    }

    /**
     * Create a new organization.
     *
     * @param request Organization creation request
     * @return Created organization DTO
     */
    public OrganizationDto createOrganization(OrganizationRequest request) {
        log.debug("Creating organization with name: {}", request.getName());
        
        if (organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name '" + request.getName() + "' already exists");
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .websiteUrl(request.getWebsiteUrl())
                .active(true)
                .build();

        Organization savedOrganization = organizationRepository.save(organization);
        log.info("Created organization with id: {} and name: {}", savedOrganization.getId(), savedOrganization.getName());
        
        return convertToDto(savedOrganization);
    }

    /**
     * Update an organization.
     *
     * @param id Organization ID
     * @param request Organization update request
     * @return Updated organization DTO
     */
    public OrganizationDto updateOrganization(Long id, OrganizationRequest request) {
        log.debug("Updating organization with id: {}", id);
        
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));

        // Check if name is being changed and if new name already exists
        if (!organization.getName().equals(request.getName()) && 
            organizationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Organization with name '" + request.getName() + "' already exists");
        }

        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization.setLogoUrl(request.getLogoUrl());
        organization.setWebsiteUrl(request.getWebsiteUrl());

        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("Updated organization with id: {}", updatedOrganization.getId());
        
        return convertToDto(updatedOrganization);
    }

    /**
     * Delete an organization.
     *
     * @param id Organization ID
     */
    public void deleteOrganization(Long id) {
        log.debug("Deleting organization with id: {}", id);
        
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));

        organizationRepository.delete(organization);
        log.info("Deleted organization with id: {}", id);
    }

    /**
     * Get all users in an organization.
     *
     * @param id Organization ID
     * @return List of user DTOs
     */
    @Transactional(readOnly = true)
    public List<UserDto> getOrganizationUsers(Long id) {
        log.debug("Fetching users for organization with id: {}", id);
        
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));

        return organization.getUsers()
                .stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());
    }

    /**
     * Add a user to an organization.
     *
     * @param organizationId Organization ID
     * @param userId User ID
     */
    public void addUserToOrganization(Long organizationId, Long userId) {
        log.debug("Adding user {} to organization {}", userId, organizationId);
        
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + organizationId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        organization.getUsers().add(user);
        user.getOrganizations().add(organization);
        
        organizationRepository.save(organization);
        log.info("Added user {} to organization {}", userId, organizationId);
    }

    /**
     * Remove a user from an organization.
     *
     * @param organizationId Organization ID
     * @param userId User ID
     */
    public void removeUserFromOrganization(Long organizationId, Long userId) {
        log.debug("Removing user {} from organization {}", userId, organizationId);
        
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + organizationId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        organization.getUsers().remove(user);
        user.getOrganizations().remove(organization);
        
        organizationRepository.save(organization);
        log.info("Removed user {} from organization {}", userId, organizationId);
    }

    /**
     * Convert Organization entity to DTO.
     *
     * @param organization Organization entity
     * @return Organization DTO
     */
    private OrganizationDto convertToDto(Organization organization) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .logoUrl(organization.getLogoUrl())
                .websiteUrl(organization.getWebsiteUrl())
                .active(organization.isActive())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    /**
     * Convert User entity to DTO.
     *
     * @param user User entity
     * @return User DTO
     */
    private UserDto convertUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
