package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.ApiExample;
import com.codebridge.documentation.service.DocumentationService;
import com.codebridge.documentation.service.InteractiveDocumentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing API examples.
 */
@RestController
@RequestMapping("/api/docs/examples")
@RequiredArgsConstructor
@Tag(name = "Examples", description = "API for managing API examples")
public class ExampleController {

    private final InteractiveDocumentationService interactiveDocumentationService;
    private final DocumentationService documentationService;

    /**
     * Get examples for a documentation.
     *
     * @param documentationId the documentation ID
     * @return the list of API examples
     */
    @GetMapping("/documentation/{documentationId}")
    @Operation(summary = "Get examples for a documentation", 
            description = "Returns all examples for a documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved examples",
                content = @Content(schema = @Schema(implementation = ApiExample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<ApiExample>> getExamplesForDocumentation(
            @Parameter(description = "Documentation ID") @PathVariable UUID documentationId) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationById(documentationId);
            return ResponseEntity.ok(interactiveDocumentationService.getExamples(documentation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get an example by ID.
     *
     * @param id the example ID
     * @return the API example
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get an example by ID", description = "Returns an example by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved example",
                content = @Content(schema = @Schema(implementation = ApiExample.class))),
        @ApiResponse(responseCode = "404", description = "Example not found")
    })
    public ResponseEntity<ApiExample> getExampleById(
            @Parameter(description = "Example ID") @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(interactiveDocumentationService.getExampleById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate examples for a documentation.
     *
     * @param documentationId the documentation ID
     * @return the list of generated API examples
     */
    @PostMapping("/documentation/{documentationId}/generate")
    @Operation(summary = "Generate examples for a documentation", 
            description = "Generates examples for a documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated examples",
                content = @Content(schema = @Schema(implementation = ApiExample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<ApiExample>> generateExamples(
            @Parameter(description = "Documentation ID") @PathVariable UUID documentationId) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationById(documentationId);
            return ResponseEntity.ok(interactiveDocumentationService.generateExamples(documentation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update an example.
     *
     * @param id the example ID
     * @param requestExample the new request example
     * @param responseExample the new response example
     * @return the updated API example
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an example", description = "Updates an example")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated example",
                content = @Content(schema = @Schema(implementation = ApiExample.class))),
        @ApiResponse(responseCode = "404", description = "Example not found")
    })
    public ResponseEntity<ApiExample> updateExample(
            @Parameter(description = "Example ID") @PathVariable UUID id,
            @Parameter(description = "New request example") @RequestParam(required = false) String requestExample,
            @Parameter(description = "New response example") @RequestParam(required = false) String responseExample) {
        try {
            return ResponseEntity.ok(interactiveDocumentationService.updateExample(id, requestExample, responseExample));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete an example.
     *
     * @param id the example ID
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an example", description = "Deletes an example")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted example"),
        @ApiResponse(responseCode = "404", description = "Example not found")
    })
    public ResponseEntity<Void> deleteExample(
            @Parameter(description = "Example ID") @PathVariable UUID id) {
        try {
            interactiveDocumentationService.deleteExample(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

