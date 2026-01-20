package com.codebridge.gitlab.git.controller;

import com.codebridge.gitlab.git.dto.GitProviderDto;
import com.codebridge.gitlab.git.model.GitProvider.ProviderType;
import com.codebridge.gitlab.git.service.GitProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
@Tag(name = "Git Providers", description = "API for managing Git providers")
public class GitProviderController {
    
    private final GitProviderService gitProviderService;
    
    @GetMapping
    @Operation(summary = "Get all Git providers")
    public ResponseEntity<List<GitProviderDto>> getAllProviders() {
        return ResponseEntity.ok(gitProviderService.getAllProviders());
    }
    
    @GetMapping("/enabled")
    @Operation(summary = "Get all enabled Git providers")
    public ResponseEntity<List<GitProviderDto>> getEnabledProviders() {
        return ResponseEntity.ok(gitProviderService.getEnabledProviders());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get Git provider by ID")
    public ResponseEntity<GitProviderDto> getProviderById(@PathVariable UUID id) {
        return ResponseEntity.ok(gitProviderService.getProviderById(id));
    }
    
    @GetMapping("/name/{name}")
    @Operation(summary = "Get Git provider by name")
    public ResponseEntity<GitProviderDto> getProviderByName(@PathVariable String name) {
        return ResponseEntity.ok(gitProviderService.getProviderByName(name));
    }
    
    @GetMapping("/type/{type}")
    @Operation(summary = "Get Git provider by type")
    public ResponseEntity<GitProviderDto> getProviderByType(@PathVariable ProviderType type) {
        return ResponseEntity.ok(gitProviderService.getProviderByType(type));
    }
    
    @PostMapping
    @Operation(summary = "Create a new Git provider")
    public ResponseEntity<GitProviderDto> createProvider(@Valid @RequestBody GitProviderDto providerDto) {
        return new ResponseEntity<>(gitProviderService.createProvider(providerDto), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing Git provider")
    public ResponseEntity<GitProviderDto> updateProvider(
            @PathVariable UUID id, 
            @Valid @RequestBody GitProviderDto providerDto) {
        return ResponseEntity.ok(gitProviderService.updateProvider(id, providerDto));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Git provider")
    public ResponseEntity<Void> deleteProvider(@PathVariable UUID id) {
        gitProviderService.deleteProvider(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable a Git provider")
    public ResponseEntity<Void> enableProvider(@PathVariable UUID id) {
        gitProviderService.enableProvider(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/disable")
    @Operation(summary = "Disable a Git provider")
    public ResponseEntity<Void> disableProvider(@PathVariable UUID id) {
        gitProviderService.disableProvider(id);
        return ResponseEntity.noContent().build();
    }
}

