package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.SearchResult;
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
 * Controller for searching API documentation.
 */
@RestController
@RequestMapping("/api/docs/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "API for searching API documentation")
public class SearchController {

    private final InteractiveDocumentationService interactiveDocumentationService;
    private final DocumentationService documentationService;

    /**
     * Search API documentation.
     *
     * @param query the search query
     * @return the list of search results
     */
    @GetMapping
    @Operation(summary = "Search API documentation", description = "Searches API documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully searched documentation",
                content = @Content(schema = @Schema(implementation = SearchResult.class)))
    })
    public ResponseEntity<List<SearchResult>> search(
            @Parameter(description = "Search query") @RequestParam String query) {
        return ResponseEntity.ok(interactiveDocumentationService.search(query));
    }

    /**
     * Build search index for a documentation.
     *
     * @param documentationId the documentation ID
     * @return the response entity
     */
    @PostMapping("/index/documentation/{documentationId}")
    @Operation(summary = "Build search index for a documentation", 
            description = "Builds search index for a documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully built search index"),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<Void> buildSearchIndexForDocumentation(
            @Parameter(description = "Documentation ID") @PathVariable UUID documentationId) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationById(documentationId);
            interactiveDocumentationService.indexDocumentation(documentation);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Build search index for all documentation.
     *
     * @return the response entity
     */
    @PostMapping("/index/all")
    @Operation(summary = "Build search index for all documentation", 
            description = "Builds search index for all documentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully built search index")
    })
    public ResponseEntity<Void> buildSearchIndexForAllDocumentation() {
        interactiveDocumentationService.buildSearchIndex();
        return ResponseEntity.noContent().build();
    }
}

