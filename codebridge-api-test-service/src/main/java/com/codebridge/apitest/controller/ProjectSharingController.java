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

@RestController
@RequestMapping("/api/projects/{projectId}/shares")
public class ProjectSharingController {

    private final ProjectSharingService projectSharingService;

    public ProjectSharingController(ProjectSharingService projectSharingService) {
        this.projectSharingService = projectSharingService;
    }

    // Helper method to extract platform user ID from authentication
    private Long getPlatformUserId(Authentication authentication) {
        if (authentication == null) {
            // For testing purposes
            return 1L;
        }
        return Long.parseLong(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ShareGrantResponse> grantProjectAccess(@PathVariable Long projectId,
                                                               @Valid @RequestBody ShareGrantRequest shareGrantRequest,
                                                               Authentication authentication) {
        Long granterUserId = getPlatformUserId(authentication);
        ShareGrantResponse response = projectSharingService.createShare(projectId, shareGrantRequest, granterUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{granteeUserId}")
    public ResponseEntity<Void> revokeProjectAccess(@PathVariable Long projectId,
                                                    @PathVariable Long granteeUserId,
                                                    Authentication authentication) {
        Long revokerUserId = getPlatformUserId(authentication);
        projectSharingService.revokeShare(projectId, granteeUserId, revokerUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ShareGrantResponse>> listSharedUsers(@PathVariable Long projectId,
                                                                  Authentication authentication) {
        Long platformUserId = getPlatformUserId(authentication); // User trying to list shares
        List<ShareGrantResponse> shares = projectSharingService.listSharesForProject(projectId, platformUserId);
        return ResponseEntity.ok(shares);
    }
}

