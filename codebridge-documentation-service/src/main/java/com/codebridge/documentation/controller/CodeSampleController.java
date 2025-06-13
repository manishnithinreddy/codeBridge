package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.CodeSample;
import com.codebridge.documentation.model.ProgrammingLanguage;
import com.codebridge.documentation.service.CodeGenerationService;
import com.codebridge.documentation.service.DocumentationService;
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
 * Controller for managing code samples.
 */
@RestController
@RequestMapping("/api/docs/code-samples")
@RequiredArgsConstructor
@Tag(name = "Code Samples", description = "API for managing code samples")
public class CodeSampleController {

    private final CodeGenerationService codeGenerationService;
    private final DocumentationService documentationService;

    /**
     * Get code samples for a documentation.
     *
     * @param documentationId the documentation ID
     * @return the list of code samples
     */
    @GetMapping("/documentation/{documentationId}")
    @Operation(summary = "Get code samples for a documentation", 
            description = "Returns all code samples for a documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved code samples",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<CodeSample>> getCodeSamplesForDocumentation(
            @Parameter(description = "Documentation ID") @PathVariable UUID documentationId) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationById(documentationId);
            return ResponseEntity.ok(codeGenerationService.getCodeSamples(documentation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get code samples for a documentation and language.
     *
     * @param documentationId the documentation ID
     * @param language the programming language
     * @return the list of code samples
     */
    @GetMapping("/documentation/{documentationId}/language/{language}")
    @Operation(summary = "Get code samples for a documentation and language", 
            description = "Returns all code samples for a documentation and language")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved code samples",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<CodeSample>> getCodeSamplesByLanguage(
            @Parameter(description = "Documentation ID") @PathVariable UUID documentationId,
            @Parameter(description = "Programming language") @PathVariable ProgrammingLanguage language) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationById(documentationId);
            return ResponseEntity.ok(codeGenerationService.getCodeSamplesByLanguage(documentation, language));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get a code sample by ID.
     *
     * @param id the code sample ID
     * @return the code sample
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a code sample by ID", description = "Returns a code sample by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved code sample",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Code sample not found")
    })
    public ResponseEntity<CodeSample> getCodeSampleById(
            @Parameter(description = "Code sample ID") @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(codeGenerationService.getCodeSampleById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate code samples for a documentation.
     *
     * @param documentationId the documentation ID
     * @return the list of generated code samples
     */
    @PostMapping("/documentation/{documentationId}/generate")
    @Operation(summary = "Generate code samples for a documentation", 
            description = "Generates code samples for a documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated code samples",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<CodeSample>> generateCodeSamples(
            @Parameter(description = "Documentation ID") @PathVariable UUID documentationId) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationById(documentationId);
            return ResponseEntity.ok(codeGenerationService.generateCodeSamples(documentation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update a code sample.
     *
     * @param id the code sample ID
     * @param code the new code
     * @return the updated code sample
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a code sample", description = "Updates a code sample")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated code sample",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Code sample not found")
    })
    public ResponseEntity<CodeSample> updateCodeSample(
            @Parameter(description = "Code sample ID") @PathVariable UUID id,
            @Parameter(description = "New code") @RequestParam String code) {
        try {
            return ResponseEntity.ok(codeGenerationService.updateCodeSample(id, code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a code sample.
     *
     * @param id the code sample ID
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a code sample", description = "Deletes a code sample")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted code sample"),
        @ApiResponse(responseCode = "404", description = "Code sample not found")
    })
    public ResponseEntity<Void> deleteCodeSample(
            @Parameter(description = "Code sample ID") @PathVariable UUID id) {
        try {
            codeGenerationService.deleteCodeSample(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

