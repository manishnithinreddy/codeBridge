package com.codebridge.documentation.controller;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.ApiExample;
import com.codebridge.documentation.model.CodeSample;
import com.codebridge.documentation.model.ProgrammingLanguage;
import com.codebridge.documentation.service.CodeGenerationService;
import com.codebridge.documentation.service.DocumentationService;
import com.codebridge.documentation.service.InteractiveDocumentationService;
import com.codebridge.documentation.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller for public API documentation.
 */
@RestController
@RequestMapping("/api/docs/public")
@RequiredArgsConstructor
@Tag(name = "Public Documentation", description = "API for accessing public API documentation")
public class PublicDocumentationController {

    private final DocumentationService documentationService;
    private final StorageService storageService;
    private final CodeGenerationService codeGenerationService;
    private final InteractiveDocumentationService interactiveDocumentationService;

    /**
     * Get OpenAPI specification for a service and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the OpenAPI specification
     */
    @GetMapping(value = "/{serviceName}/{versionName}/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get OpenAPI specification", 
            description = "Returns the OpenAPI specification for a service and version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved OpenAPI specification"),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<String> getOpenApiSpec(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationByServiceAndVersion(
                    serviceName, versionName);
            return ResponseEntity.ok(documentation.getOpenApiSpec());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get HTML documentation for a service and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the HTML documentation
     */
    @GetMapping(value = "/{serviceName}/{versionName}/index.html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get HTML documentation", 
            description = "Returns the HTML documentation for a service and version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved HTML documentation"),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<String> getHtmlDocs(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationByServiceAndVersion(
                    serviceName, versionName);
            
            if (documentation.getHtmlPath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            try {
                String html = storageService.readFile(documentation.getHtmlPath());
                return ResponseEntity.ok(html);
            } catch (IOException e) {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get Markdown documentation for a service and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the Markdown documentation
     */
    @GetMapping(value = "/{serviceName}/{versionName}/README.md", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get Markdown documentation", 
            description = "Returns the Markdown documentation for a service and version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Markdown documentation"),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<String> getMarkdownDocs(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationByServiceAndVersion(
                    serviceName, versionName);
            
            if (documentation.getMarkdownPath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            try {
                String markdown = storageService.readFile(documentation.getMarkdownPath());
                return ResponseEntity.ok(markdown);
            } catch (IOException e) {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get code samples for a service and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the list of code samples
     */
    @GetMapping("/{serviceName}/{versionName}/code-samples")
    @Operation(summary = "Get code samples", 
            description = "Returns the code samples for a service and version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved code samples",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<CodeSample>> getCodeSamples(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationByServiceAndVersion(
                    serviceName, versionName);
            return ResponseEntity.ok(codeGenerationService.getCodeSamples(documentation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get code samples for a service, version, and language.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @param language the programming language
     * @return the list of code samples
     */
    @GetMapping("/{serviceName}/{versionName}/code-samples/{language}")
    @Operation(summary = "Get code samples by language", 
            description = "Returns the code samples for a service, version, and language")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved code samples",
                content = @Content(schema = @Schema(implementation = CodeSample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<CodeSample>> getCodeSamplesByLanguage(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName,
            @Parameter(description = "Programming language") @PathVariable ProgrammingLanguage language) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationByServiceAndVersion(
                    serviceName, versionName);
            return ResponseEntity.ok(codeGenerationService.getCodeSamplesByLanguage(documentation, language));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get examples for a service and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the list of API examples
     */
    @GetMapping("/{serviceName}/{versionName}/examples")
    @Operation(summary = "Get examples", 
            description = "Returns the examples for a service and version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved examples",
                content = @Content(schema = @Schema(implementation = ApiExample.class))),
        @ApiResponse(responseCode = "404", description = "Documentation not found")
    })
    public ResponseEntity<List<ApiExample>> getExamples(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @Parameter(description = "Version name") @PathVariable String versionName) {
        try {
            ApiDocumentation documentation = documentationService.getDocumentationByServiceAndVersion(
                    serviceName, versionName);
            return ResponseEntity.ok(interactiveDocumentationService.getExamples(documentation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

