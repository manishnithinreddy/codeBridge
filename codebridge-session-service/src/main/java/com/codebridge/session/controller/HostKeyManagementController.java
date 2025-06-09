package com.codebridge.session.controller;

import com.codebridge.session.dto.HostKeyDTO;
import com.codebridge.session.dto.HostKeyListResponse;
import com.codebridge.session.dto.HostKeyVerificationPolicyDTO;
import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.codebridge.session.service.CustomJschHostKeyRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing SSH host keys
 */
@RestController
@RequestMapping("/api/host-keys")
public class HostKeyManagementController {

    private static final Logger logger = LoggerFactory.getLogger(HostKeyManagementController.class);
    
    private final KnownSshHostKeyRepository hostKeyRepository;
    private final CustomJschHostKeyRepository customJschHostKeyRepository;
    
    public HostKeyManagementController(
            KnownSshHostKeyRepository hostKeyRepository,
            CustomJschHostKeyRepository customJschHostKeyRepository) {
        this.hostKeyRepository = hostKeyRepository;
        this.customJschHostKeyRepository = customJschHostKeyRepository;
    }
    
    /**
     * Get all known host keys
     * @return List of host keys
     */
    @GetMapping
    public ResponseEntity<HostKeyListResponse> getAllHostKeys() {
        List<KnownSshHostKey> hostKeys = hostKeyRepository.findAll();
        List<HostKeyDTO> hostKeyDTOs = hostKeys.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        HostKeyListResponse response = new HostKeyListResponse(
                hostKeyDTOs,
                customJschHostKeyRepository.getVerificationPolicy().toString()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get host keys for a specific host
     * @param hostname Hostname
     * @param port Port (optional, defaults to 22)
     * @return List of host keys for the specified host
     */
    @GetMapping("/host")
    public ResponseEntity<HostKeyListResponse> getHostKeysByHost(
            @RequestParam @NotBlank String hostname,
            @RequestParam(required = false, defaultValue = "22") int port) {
        
        List<KnownSshHostKey> hostKeys = hostKeyRepository.findByHostnameAndPort(hostname, port);
        List<HostKeyDTO> hostKeyDTOs = hostKeys.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        HostKeyListResponse response = new HostKeyListResponse(
                hostKeyDTOs,
                customJschHostKeyRepository.getVerificationPolicy().toString()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get a specific host key by ID
     * @param id Host key ID
     * @return Host key
     */
    @GetMapping("/{id}")
    public ResponseEntity<HostKeyDTO> getHostKeyById(@PathVariable UUID id) {
        KnownSshHostKey hostKey = hostKeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Host key not found"));
        
        return ResponseEntity.ok(mapToDTO(hostKey));
    }
    
    /**
     * Delete a host key
     * @param id Host key ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHostKey(@PathVariable UUID id) {
        KnownSshHostKey hostKey = hostKeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Host key not found"));
        
        hostKeyRepository.delete(hostKey);
        logger.info("Deleted host key for [{}:{}] type {}", 
                hostKey.getHostname(), hostKey.getPort(), hostKey.getKeyType());
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete all host keys for a specific host
     * @param hostname Hostname
     * @param port Port (optional, defaults to 22)
     * @return No content
     */
    @DeleteMapping("/host")
    public ResponseEntity<Void> deleteHostKeysByHost(
            @RequestParam @NotBlank String hostname,
            @RequestParam(required = false, defaultValue = "22") int port) {
        
        List<KnownSshHostKey> hostKeys = hostKeyRepository.findByHostnameAndPort(hostname, port);
        if (!hostKeys.isEmpty()) {
            hostKeyRepository.deleteAll(hostKeys);
            logger.info("Deleted all host keys for [{}:{}]", hostname, port);
        }
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Add or update a host key
     * @param hostKeyDTO Host key data
     * @return Created host key
     */
    @PostMapping
    public ResponseEntity<HostKeyDTO> addHostKey(@RequestBody @Valid HostKeyDTO hostKeyDTO) {
        // Check if a host key with the same hostname, port, and key type already exists
        Optional<KnownSshHostKey> existingKeyOpt = hostKeyRepository.findByHostnameAndPortAndKeyType(
                hostKeyDTO.getHostname(), hostKeyDTO.getPort(), hostKeyDTO.getKeyType());
        
        KnownSshHostKey hostKey = existingKeyOpt.orElse(new KnownSshHostKey());
        hostKey.setHostname(hostKeyDTO.getHostname());
        hostKey.setPort(hostKeyDTO.getPort());
        hostKey.setKeyType(hostKeyDTO.getKeyType());
        hostKey.setHostKeyBase64(hostKeyDTO.getHostKeyBase64());
        hostKey.setFingerprintSha256(hostKeyDTO.getFingerprintSha256());
        hostKey.setLastVerified(LocalDateTime.now());
        
        // Only set firstSeen for new keys
        if (!existingKeyOpt.isPresent()) {
            hostKey.setFirstSeen(LocalDateTime.now());
        }
        
        KnownSshHostKey savedHostKey = hostKeyRepository.save(hostKey);
        logger.info("{} host key for [{}:{}] type {}", 
                existingKeyOpt.isPresent() ? "Updated" : "Added", 
                hostKey.getHostname(), hostKey.getPort(), hostKey.getKeyType());
        
        return ResponseEntity.status(existingKeyOpt.isPresent() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(mapToDTO(savedHostKey));
    }
    
    /**
     * Get the current host key verification policy
     * @return Current policy
     */
    @GetMapping("/policy")
    public ResponseEntity<HostKeyVerificationPolicyDTO> getVerificationPolicy() {
        HostKeyVerificationPolicyDTO policyDTO = new HostKeyVerificationPolicyDTO(
                customJschHostKeyRepository.getVerificationPolicy().toString());
        
        return ResponseEntity.ok(policyDTO);
    }
    
    /**
     * Set the host key verification policy
     * @param policyDTO Policy to set
     * @return Updated policy
     */
    @PutMapping("/policy")
    public ResponseEntity<HostKeyVerificationPolicyDTO> setVerificationPolicy(
            @RequestBody @Valid HostKeyVerificationPolicyDTO policyDTO) {
        
        try {
            CustomJschHostKeyRepository.HostKeyVerificationPolicy policy = 
                    CustomJschHostKeyRepository.HostKeyVerificationPolicy.valueOf(policyDTO.getPolicy());
            
            customJschHostKeyRepository.setVerificationPolicy(policy);
            logger.info("Host key verification policy set to: {}", policy);
            
            return ResponseEntity.ok(policyDTO);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid policy. Valid values are: STRICT, ASK, AUTO_ACCEPT");
        }
    }
    
    /**
     * Map a KnownSshHostKey entity to a HostKeyDTO
     * @param hostKey Entity to map
     * @return Mapped DTO
     */
    private HostKeyDTO mapToDTO(KnownSshHostKey hostKey) {
        return new HostKeyDTO(
                hostKey.getId(),
                hostKey.getHostname(),
                hostKey.getPort(),
                hostKey.getKeyType(),
                hostKey.getHostKeyBase64(),
                hostKey.getFingerprintSha256(),
                hostKey.getFirstSeen(),
                hostKey.getLastVerified()
        );
    }
}

