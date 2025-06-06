package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.EnvironmentRequest;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.Environment;
import com.codebridge.apitest.repository.EnvironmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for environment operations.
 */
@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ObjectMapper objectMapper;

    public EnvironmentService(EnvironmentRepository environmentRepository, ObjectMapper objectMapper) {
        this.environmentRepository = environmentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new environment.
     *
     * @param request the environment request
     * @param userId the user ID
     * @return the created environment
     */
    @Transactional
    public EnvironmentResponse createEnvironment(EnvironmentRequest request, UUID userId) {
        Environment environment = new Environment();
        environment.setId(UUID.randomUUID());
        environment.setName(request.getName());
        environment.setDescription(request.getDescription());
        environment.setUserId(userId);
        
        try {
            if (request.getVariables() != null) {
                environment.setVariables(objectMapper.writeValueAsString(request.getVariables()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing variables JSON", e);
        }
        
        environment.setDefault(request.isDefault());
        
        // If this is set as default, unset any existing default
        if (request.isDefault()) {
            environmentRepository.findByUserIdAndIsDefault(userId, true)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        environmentRepository.save(existingDefault);
                    });
        }
        
        Environment savedEnvironment = environmentRepository.save(environment);
        return mapToEnvironmentResponse(savedEnvironment);
    }

    /**
     * Get all environments for a user.
     *
     * @param userId the user ID
     * @return list of environments
     */
    public List<EnvironmentResponse> getAllEnvironments(UUID userId) {
        List<Environment> environments = environmentRepository.findByUserId(userId);
        return environments.stream()
                .map(this::mapToEnvironmentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get environment by ID.
     *
     * @param id the environment ID
     * @param userId the user ID
     * @return the environment
     */
    public EnvironmentResponse getEnvironmentById(UUID id, UUID userId) {
        Environment environment = environmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));
        return mapToEnvironmentResponse(environment);
    }

    /**
     * Update an environment.
     *
     * @param id the environment ID
     * @param request the environment request
     * @param userId the user ID
     * @return the updated environment
     */
    @Transactional
    public EnvironmentResponse updateEnvironment(UUID id, EnvironmentRequest request, UUID userId) {
        Environment environment = environmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));
        
        environment.setName(request.getName());
        environment.setDescription(request.getDescription());
        
        try {
            if (request.getVariables() != null) {
                environment.setVariables(objectMapper.writeValueAsString(request.getVariables()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing variables JSON", e);
        }
        
        // Handle default status change
        if (request.isDefault() && !environment.isDefault()) {
            environmentRepository.findByUserIdAndIsDefault(userId, true)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        environmentRepository.save(existingDefault);
                    });
            environment.setDefault(true);
        } else if (!request.isDefault() && environment.isDefault()) {
            // Don't allow removing default status without setting another default
            Optional<Environment> otherEnvironment = environmentRepository.findByUserId(userId).stream()
                    .filter(e -> !e.getId().equals(id))
                    .findFirst();
            
            if (otherEnvironment.isPresent()) {
                Environment newDefault = otherEnvironment.get();
                newDefault.setDefault(true);
                environmentRepository.save(newDefault);
                environment.setDefault(false);
            }
        }
        
        Environment updatedEnvironment = environmentRepository.save(environment);
        return mapToEnvironmentResponse(updatedEnvironment);
    }

    /**
     * Delete an environment.
     *
     * @param id the environment ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteEnvironment(UUID id, UUID userId) {
        Environment environment = environmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", "id", id));
        
        // If this is the default environment, set another one as default
        if (environment.isDefault()) {
            Optional<Environment> otherEnvironment = environmentRepository.findByUserId(userId).stream()
                    .filter(e -> !e.getId().equals(id))
                    .findFirst();
            
            if (otherEnvironment.isPresent()) {
                Environment newDefault = otherEnvironment.get();
                newDefault.setDefault(true);
                environmentRepository.save(newDefault);
            }
        }
        
        environmentRepository.delete(environment);
    }

    /**
     * Get the default environment for a user.
     *
     * @param userId the user ID
     * @return the default environment
     */
    public EnvironmentResponse getDefaultEnvironment(UUID userId) {
        Environment environment = environmentRepository.findByUserIdAndIsDefault(userId, true)
                .orElseGet(() -> {
                    // If no default environment exists, create one
                    Environment newDefault = new Environment();
                    newDefault.setId(UUID.randomUUID());
                    newDefault.setName("Default Environment");
                    newDefault.setDescription("Default environment created automatically");
                    newDefault.setUserId(userId);
                    newDefault.setDefault(true);
                    try {
                        newDefault.setVariables(objectMapper.writeValueAsString(Map.of()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error processing variables JSON", e);
                    }
                    return environmentRepository.save(newDefault);
                });
        
        return mapToEnvironmentResponse(environment);
    }

    /**
     * Maps an Environment entity to an EnvironmentResponse DTO.
     *
     * @param environment the environment entity
     * @return the environment response DTO
     */
    private EnvironmentResponse mapToEnvironmentResponse(Environment environment) {
        EnvironmentResponse response = new EnvironmentResponse();
        response.setId(environment.getId());
        response.setName(environment.getName());
        response.setDescription(environment.getDescription());
        
        if (environment.getVariables() != null) {
            try {
                response.setVariables(objectMapper.readValue(environment.getVariables(), 
                        new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing variables JSON", e);
            }
        }
        
        response.setDefault(environment.isDefault());
        response.setCreatedAt(environment.getCreatedAt());
        response.setUpdatedAt(environment.getUpdatedAt());
        
        return response;
    }
}

