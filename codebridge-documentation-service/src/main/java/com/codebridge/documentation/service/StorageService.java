package com.codebridge.documentation.service;

import com.codebridge.documentation.model.ApiDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for storing and retrieving documentation files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final OpenApiService openApiService;

    @Value("${documentation.storage.base-path}")
    private String basePath;

    /**
     * Store OpenAPI specification.
     *
     * @param documentation the API documentation
     * @return the path to the stored file
     * @throws IOException if an I/O error occurs
     */
    public String storeOpenApiSpec(ApiDocumentation documentation) throws IOException {
        String serviceName = documentation.getService().getName();
        String versionName = documentation.getVersion().getName();
        String fileName = "openapi.json";
        
        Path dirPath = createDirectoryStructure(serviceName, versionName);
        Path filePath = dirPath.resolve(fileName);
        
        Files.writeString(filePath, documentation.getOpenApiSpec(), StandardCharsets.UTF_8);
        
        return filePath.toString();
    }

    /**
     * Generate and store HTML documentation.
     *
     * @param documentation the API documentation
     * @return the path to the stored file
     * @throws IOException if an I/O error occurs
     */
    public String generateAndStoreHtmlDocs(ApiDocumentation documentation) throws IOException {
        String serviceName = documentation.getService().getName();
        String versionName = documentation.getVersion().getName();
        String fileName = "index.html";
        
        Path dirPath = createDirectoryStructure(serviceName, versionName);
        Path filePath = dirPath.resolve(fileName);
        
        // Generate HTML documentation
        String html = generateHtmlDocumentation(documentation);
        
        Files.writeString(filePath, html, StandardCharsets.UTF_8);
        
        return filePath.toString();
    }

    /**
     * Generate and store Markdown documentation.
     *
     * @param documentation the API documentation
     * @return the path to the stored file
     * @throws IOException if an I/O error occurs
     */
    public String generateAndStoreMarkdownDocs(ApiDocumentation documentation) throws IOException {
        String serviceName = documentation.getService().getName();
        String versionName = documentation.getVersion().getName();
        String fileName = "README.md";
        
        Path dirPath = createDirectoryStructure(serviceName, versionName);
        Path filePath = dirPath.resolve(fileName);
        
        // Generate Markdown documentation
        String markdown = generateMarkdownDocumentation(documentation);
        
        Files.writeString(filePath, markdown, StandardCharsets.UTF_8);
        
        return filePath.toString();
    }

    /**
     * Create directory structure for storing documentation.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the path to the created directory
     * @throws IOException if an I/O error occurs
     */
    private Path createDirectoryStructure(String serviceName, String versionName) throws IOException {
        Path path = Paths.get(basePath, serviceName, versionName);
        Files.createDirectories(path);
        return path;
    }

    /**
     * Generate HTML documentation from OpenAPI specification.
     *
     * @param documentation the API documentation
     * @return the generated HTML
     */
    private String generateHtmlDocumentation(ApiDocumentation documentation) {
        try {
            OpenAPI openAPI = openApiService.parseOpenApiSpec(documentation.getOpenApiSpec());
            if (openAPI == null) {
                return "<html><body><h1>Error</h1><p>Failed to parse OpenAPI specification.</p></body></html>";
            }
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("  <meta charset=\"UTF-8\">\n");
            html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("  <title>").append(openAPI.getInfo().getTitle()).append(" - API Documentation</title>\n");
            html.append("  <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css\">\n");
            html.append("  <style>\n");
            html.append("    body { padding: 20px; }\n");
            html.append("    .endpoint { margin-bottom: 30px; }\n");
            html.append("    .method { font-weight: bold; }\n");
            html.append("    .path { font-family: monospace; }\n");
            html.append("    .get { color: #0d6efd; }\n");
            html.append("    .post { color: #198754; }\n");
            html.append("    .put { color: #fd7e14; }\n");
            html.append("    .delete { color: #dc3545; }\n");
            html.append("    .patch { color: #6f42c1; }\n");
            html.append("  </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("  <div class=\"container\">\n");
            html.append("    <h1>").append(openAPI.getInfo().getTitle()).append("</h1>\n");
            html.append("    <p class=\"lead\">").append(openAPI.getInfo().getDescription()).append("</p>\n");
            html.append("    <p><strong>Version:</strong> ").append(openAPI.getInfo().getVersion()).append("</p>\n");
            
            if (openAPI.getPaths() != null) {
                html.append("    <h2>Endpoints</h2>\n");
                openAPI.getPaths().forEach((path, pathItem) -> {
                    if (pathItem.getGet() != null) {
                        html.append("    <div class=\"endpoint\">\n");
                        html.append("      <h3><span class=\"method get\">GET</span> <span class=\"path\">").append(path).append("</span></h3>\n");
                        html.append("      <p>").append(pathItem.getGet().getSummary()).append("</p>\n");
                        html.append("      <p>").append(pathItem.getGet().getDescription()).append("</p>\n");
                        html.append("    </div>\n");
                    }
                    if (pathItem.getPost() != null) {
                        html.append("    <div class=\"endpoint\">\n");
                        html.append("      <h3><span class=\"method post\">POST</span> <span class=\"path\">").append(path).append("</span></h3>\n");
                        html.append("      <p>").append(pathItem.getPost().getSummary()).append("</p>\n");
                        html.append("      <p>").append(pathItem.getPost().getDescription()).append("</p>\n");
                        html.append("    </div>\n");
                    }
                    if (pathItem.getPut() != null) {
                        html.append("    <div class=\"endpoint\">\n");
                        html.append("      <h3><span class=\"method put\">PUT</span> <span class=\"path\">").append(path).append("</span></h3>\n");
                        html.append("      <p>").append(pathItem.getPut().getSummary()).append("</p>\n");
                        html.append("      <p>").append(pathItem.getPut().getDescription()).append("</p>\n");
                        html.append("    </div>\n");
                    }
                    if (pathItem.getDelete() != null) {
                        html.append("    <div class=\"endpoint\">\n");
                        html.append("      <h3><span class=\"method delete\">DELETE</span> <span class=\"path\">").append(path).append("</span></h3>\n");
                        html.append("      <p>").append(pathItem.getDelete().getSummary()).append("</p>\n");
                        html.append("      <p>").append(pathItem.getDelete().getDescription()).append("</p>\n");
                        html.append("    </div>\n");
                    }
                    if (pathItem.getPatch() != null) {
                        html.append("    <div class=\"endpoint\">\n");
                        html.append("      <h3><span class=\"method patch\">PATCH</span> <span class=\"path\">").append(path).append("</span></h3>\n");
                        html.append("      <p>").append(pathItem.getPatch().getSummary()).append("</p>\n");
                        html.append("      <p>").append(pathItem.getPatch().getDescription()).append("</p>\n");
                        html.append("    </div>\n");
                    }
                });
            }
            
            html.append("    <hr>\n");
            html.append("    <p class=\"text-muted\">Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>\n");
            html.append("  </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            return html.toString();
        } catch (Exception e) {
            log.error("Error generating HTML documentation: {}", e.getMessage(), e);
            return "<html><body><h1>Error</h1><p>Failed to generate documentation: " + e.getMessage() + "</p></body></html>";
        }
    }

    /**
     * Generate Markdown documentation from OpenAPI specification.
     *
     * @param documentation the API documentation
     * @return the generated Markdown
     */
    private String generateMarkdownDocumentation(ApiDocumentation documentation) {
        try {
            OpenAPI openAPI = openApiService.parseOpenApiSpec(documentation.getOpenApiSpec());
            if (openAPI == null) {
                return "# Error\n\nFailed to parse OpenAPI specification.";
            }
            
            StringBuilder markdown = new StringBuilder();
            markdown.append("# ").append(openAPI.getInfo().getTitle()).append("\n\n");
            markdown.append(openAPI.getInfo().getDescription()).append("\n\n");
            markdown.append("**Version:** ").append(openAPI.getInfo().getVersion()).append("\n\n");
            
            if (openAPI.getPaths() != null) {
                markdown.append("## Endpoints\n\n");
                openAPI.getPaths().forEach((path, pathItem) -> {
                    if (pathItem.getGet() != null) {
                        markdown.append("### `GET ").append(path).append("`\n\n");
                        markdown.append(pathItem.getGet().getSummary()).append("\n\n");
                        markdown.append(pathItem.getGet().getDescription()).append("\n\n");
                    }
                    if (pathItem.getPost() != null) {
                        markdown.append("### `POST ").append(path).append("`\n\n");
                        markdown.append(pathItem.getPost().getSummary()).append("\n\n");
                        markdown.append(pathItem.getPost().getDescription()).append("\n\n");
                    }
                    if (pathItem.getPut() != null) {
                        markdown.append("### `PUT ").append(path).append("`\n\n");
                        markdown.append(pathItem.getPut().getSummary()).append("\n\n");
                        markdown.append(pathItem.getPut().getDescription()).append("\n\n");
                    }
                    if (pathItem.getDelete() != null) {
                        markdown.append("### `DELETE ").append(path).append("`\n\n");
                        markdown.append(pathItem.getDelete().getSummary()).append("\n\n");
                        markdown.append(pathItem.getDelete().getDescription()).append("\n\n");
                    }
                    if (pathItem.getPatch() != null) {
                        markdown.append("### `PATCH ").append(path).append("`\n\n");
                        markdown.append(pathItem.getPatch().getSummary()).append("\n\n");
                        markdown.append(pathItem.getPatch().getDescription()).append("\n\n");
                    }
                });
            }
            
            markdown.append("---\n\n");
            markdown.append("Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return markdown.toString();
        } catch (Exception e) {
            log.error("Error generating Markdown documentation: {}", e.getMessage(), e);
            return "# Error\n\nFailed to generate documentation: " + e.getMessage();
        }
    }

    /**
     * Read a file as a string.
     *
     * @param path the file path
     * @return the file content as a string
     * @throws IOException if an I/O error occurs
     */
    public String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }

    /**
     * Convert Markdown to HTML.
     *
     * @param markdown the Markdown content
     * @return the HTML content
     */
    public String markdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    /**
     * Convert AsciiDoc to HTML.
     *
     * @param asciidoc the AsciiDoc content
     * @return the HTML content
     */
    public String asciidocToHtml(String asciidoc) {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            Options options = Options.builder()
                    .safe(org.asciidoctor.SafeMode.SAFE)
                    .build();
            return asciidoctor.convert(asciidoc, options);
        }
    }
}

