package com.codebridge.gitlab.git.controller;

import com.codebridge.gitlab.git.dto.ShareStashRequest;
import com.codebridge.gitlab.git.dto.SharedStashDTO;
import com.codebridge.gitlab.git.mapper.SharedStashMapper;
import com.codebridge.gitlab.git.model.SharedStash;
import com.codebridge.gitlab.git.service.SharedStashService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for shared stash operations.
 */
@RestController
@RequestMapping("/api/gitlab/shared-stashes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shared Stashes", description = "API for managing shared Git stashes")
public class SharedStashController {

    private final SharedStashService sharedStashService;
    private final SharedStashMapper sharedStashMapper;

    /**
     * Register a stash for sharing.
     *
     * @param request The request containing stash information
     * @return The created shared stash
     */
    @PostMapping
    @Operation(
        summary = "Share a stash",
        description = "Register a Git stash for sharing with other users",
        responses = {
            @ApiResponse(responseCode = "201", description = "Stash shared successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Repository not found")
        }
    )
    public ResponseEntity<SharedStashDTO> shareStash(
            @Parameter(description = "Stash information", required = true)
            @Valid @RequestBody ShareStashRequest request) {
        
        log.debug("Received request to share stash: {}", request);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        
        SharedStash sharedStash = sharedStashService.registerSharedStash(
                request.getStashHash(),
                request.getRepositoryId(),
                request.getDescription(),
                currentUser,
                request.getBranch()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(sharedStashMapper.toDTO(sharedStash));
    }

    /**
     * Get all shared stashes for a repository.
     *
     * @param repositoryId The ID of the repository
     * @return A list of shared stashes
     */
    @GetMapping
    @Operation(
        summary = "List shared stashes",
        description = "Get a list of all shared stashes for a repository",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "List of shared stashes",
                content = @Content(schema = @Schema(implementation = SharedStashDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Repository not found")
        }
    )
    public ResponseEntity<List<SharedStashDTO>> listSharedStashes(
            @Parameter(description = "Repository ID", required = true)
            @RequestParam Long repositoryId) {
        
        log.debug("Listing shared stashes for repository: {}", repositoryId);
        
        List<SharedStash> sharedStashes = sharedStashService.getSharedStashes(repositoryId);
        
        return ResponseEntity.ok(sharedStashMapper.toDTOList(sharedStashes));
    }

    /**
     * Get a specific shared stash.
     *
     * @param id The ID of the shared stash
     * @return The shared stash
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get a shared stash",
        description = "Get a specific shared stash by its ID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The shared stash",
                content = @Content(schema = @Schema(implementation = SharedStashDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Shared stash not found")
        }
    )
    public ResponseEntity<SharedStashDTO> getSharedStash(
            @Parameter(description = "Shared stash ID", required = true)
            @PathVariable Long id) {
        
        log.debug("Getting shared stash with ID: {}", id);
        
        SharedStash sharedStash = sharedStashService.getSharedStash(id);
        
        return ResponseEntity.ok(sharedStashMapper.toDTO(sharedStash));
    }

    /**
     * Delete a shared stash.
     *
     * @param id The ID of the shared stash to delete
     * @return No content
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a shared stash",
        description = "Delete a specific shared stash by its ID",
        responses = {
            @ApiResponse(responseCode = "204", description = "Shared stash deleted"),
            @ApiResponse(responseCode = "404", description = "Shared stash not found")
        }
    )
    public ResponseEntity<Void> deleteSharedStash(
            @Parameter(description = "Shared stash ID", required = true)
            @PathVariable Long id) {
        
        log.debug("Deleting shared stash with ID: {}", id);
        
        sharedStashService.deleteSharedStash(id);
        
        return ResponseEntity.noContent().build();
    }
}

