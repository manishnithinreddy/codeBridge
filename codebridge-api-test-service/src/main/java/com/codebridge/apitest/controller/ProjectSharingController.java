package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ShareGrantRequest;
import com.codebridge.apitest.dto.ShareGrantResponse;
import com.codebridge.apitest.service.ProjectSharingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/shares")
public class ProjectSharingController {

    private final ProjectSharingService projectSharingService;

    public ProjectSharingController(ProjectSharingService projectSharingService) {
        this.projectSharingService = projectSharingService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ShareGrantResponse> grantProjectAccess(@PathVariable UUID projectId,
                                                                 @Valid @RequestBody ShareGrantRequest shareGrantRequest,
                                                                 Authentication authentication) {
        UUID granterUserId = getPlatformUserId(authentication);
        ShareGrantResponse response = projectSharingService.grantProjectAccess(projectId, shareGrantRequest, granterUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/users/{granteeUserId}")
    public ResponseEntity<Void> revokeProjectAccess(@PathVariable UUID projectId,
                                                    @PathVariable UUID granteeUserId,
                                                    Authentication authentication) {
        UUID revokerUserId = getPlatformUserId(authentication);
        projectSharingService.revokeProjectAccess(projectId, granteeUserId, revokerUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ShareGrantResponse>> listSharedUsers(@PathVariable UUID projectId,
                                                                    Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication); // User trying to list shares
        List<ShareGrantResponse> users = projectSharingService.listUsersForProject(projectId, platformUserId);
        return ResponseEntity.ok(users);
    }
}
