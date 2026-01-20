package com.codebridge.documentation.service;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.CodeSample;
import com.codebridge.documentation.model.ProgrammingLanguage;
import com.codebridge.documentation.repository.CodeSampleRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * Service for generating code samples and client libraries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeGenerationService {

    private final CodeSampleRepository codeSampleRepository;
    private final OpenApiService openApiService;
    private final StorageService storageService;

    @Value("${documentation.code-generation.enabled:true}")
    private boolean codeGenerationEnabled;

    @Value("${documentation.code-generation.languages}")
    private List<String> supportedLanguages;

    @Value("${documentation.code-generation.templates-path}")
    private String templatesPath;

    @Value("${documentation.storage.base-path}")
    private String basePath;

    /**
     * Generate code samples for an API documentation.
     *
     * @param documentation the API documentation
     * @return the list of generated code samples
     */
    @Transactional
    public List<CodeSample> generateCodeSamples(ApiDocumentation documentation) {
        if (!codeGenerationEnabled) {
            log.info("Code generation is disabled. Skipping code sample generation.");
            return Collections.emptyList();
        }

        log.info("Generating code samples for service: {} version: {}", 
                documentation.getService().getName(), documentation.getVersion().getName());

        List<CodeSample> samples = new ArrayList<>();
        
        try {
            OpenAPI openAPI = openApiService.parseOpenApiSpec(documentation.getOpenApiSpec());
            if (openAPI == null || openAPI.getPaths() == null) {
                log.warn("Invalid OpenAPI specification. Cannot generate code samples.");
                return Collections.emptyList();
            }
            
            // Generate samples for each endpoint and language
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();
                
                // Process each HTTP method
                processOperation(documentation, samples, path, "GET", pathItem.getGet());
                processOperation(documentation, samples, path, "POST", pathItem.getPost());
                processOperation(documentation, samples, path, "PUT", pathItem.getPut());
                processOperation(documentation, samples, path, "DELETE", pathItem.getDelete());
                processOperation(documentation, samples, path, "PATCH", pathItem.getPatch());
            }
            
            // Generate client libraries for each language
            generateClientLibraries(documentation);
            
            return samples;
        } catch (Exception e) {
            log.error("Error generating code samples: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Process an operation to generate code samples.
     *
     * @param documentation the API documentation
     * @param samples the list of code samples
     * @param path the API path
     * @param method the HTTP method
     * @param operation the operation
     */
    private void processOperation(ApiDocumentation documentation, List<CodeSample> samples, 
                                 String path, String method, Operation operation) {
        if (operation == null) {
            return;
        }
        
        String operationId = operation.getOperationId();
        if (operationId == null) {
            operationId = method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "");
        }
        
        for (String language : supportedLanguages) {
            try {
                ProgrammingLanguage lang = ProgrammingLanguage.valueOf(language.toUpperCase());
                String code = generateCodeForOperation(documentation, path, method, operation, lang);
                
                if (code != null && !code.isEmpty()) {
                    CodeSample sample = createOrUpdateCodeSample(documentation, operationId, path, method, lang, code);
                    samples.add(sample);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Unsupported language: {}", language);
            } catch (Exception e) {
                log.error("Error generating code sample for {}.{}: {}", path, method, e.getMessage(), e);
            }
        }
    }

    /**
     * Generate code for an operation.
     *
     * @param documentation the API documentation
     * @param path the API path
     * @param method the HTTP method
     * @param operation the operation
     * @param language the programming language
     * @return the generated code
     */
    private String generateCodeForOperation(ApiDocumentation documentation, String path, String method, 
                                          Operation operation, ProgrammingLanguage language) {
        // In a real implementation, this would use templates or a code generation library
        // to generate code samples for each language
        
        String baseUrl = documentation.getService().getUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        String contextPath = documentation.getService().getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
            baseUrl += contextPath;
        }
        
        String fullUrl = baseUrl + path;
        
        switch (language) {
            case JAVA:
                return generateJavaCode(fullUrl, method, operation);
            case PYTHON:
                return generatePythonCode(fullUrl, method, operation);
            case JAVASCRIPT:
                return generateJavaScriptCode(fullUrl, method, operation);
            case TYPESCRIPT:
                return generateTypeScriptCode(fullUrl, method, operation);
            case CSHARP:
                return generateCSharpCode(fullUrl, method, operation);
            case GO:
                return generateGoCode(fullUrl, method, operation);
            default:
                return null;
        }
    }

    /**
     * Generate Java code.
     *
     * @param url the API URL
     * @param method the HTTP method
     * @param operation the operation
     * @return the generated code
     */
    private String generateJavaCode(String url, String method, Operation operation) {
        StringBuilder code = new StringBuilder();
        code.append("import java.net.URI;\n");
        code.append("import java.net.http.HttpClient;\n");
        code.append("import java.net.http.HttpRequest;\n");
        code.append("import java.net.http.HttpResponse;\n");
        code.append("import java.time.Duration;\n\n");
        
        code.append("public class ApiClient {\n");
        code.append("    public static void main(String[] args) throws Exception {\n");
        code.append("        HttpClient client = HttpClient.newBuilder()\n");
        code.append("                .version(HttpClient.Version.HTTP_2)\n");
        code.append("                .connectTimeout(Duration.ofSeconds(10))\n");
        code.append("                .build();\n\n");
        
        code.append("        HttpRequest request = HttpRequest.newBuilder()\n");
        code.append("                .uri(URI.create(\"").append(url).append("\"))\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("                .header(\"Content-Type\", \"application/json\")\n");
            code.append("                .").append(method.toLowerCase()).append("(HttpRequest.BodyPublishers.ofString(\"{}\"))\n");
        } else {
            code.append("                .method(\"").append(method).append("\", HttpRequest.BodyPublishers.noBody())\n");
        }
        
        code.append("                .build();\n\n");
        
        code.append("        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());\n");
        code.append("        System.out.println(\"Status code: \" + response.statusCode());\n");
        code.append("        System.out.println(\"Response body: \" + response.body());\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }

    /**
     * Generate Python code.
     *
     * @param url the API URL
     * @param method the HTTP method
     * @param operation the operation
     * @return the generated code
     */
    private String generatePythonCode(String url, String method, Operation operation) {
        StringBuilder code = new StringBuilder();
        code.append("import requests\n\n");
        
        code.append("url = \"").append(url).append("\"\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("headers = {\"Content-Type\": \"application/json\"}\n");
            code.append("data = {}\n\n");
            code.append("response = requests.").append(method.toLowerCase()).append("(url, headers=headers, json=data)\n");
        } else {
            code.append("response = requests.").append(method.toLowerCase()).append("(url)\n");
        }
        
        code.append("print(f\"Status code: {response.status_code}\")\n");
        code.append("print(f\"Response body: {response.text}\")\n");
        
        return code.toString();
    }

    /**
     * Generate JavaScript code.
     *
     * @param url the API URL
     * @param method the HTTP method
     * @param operation the operation
     * @return the generated code
     */
    private String generateJavaScriptCode(String url, String method, Operation operation) {
        StringBuilder code = new StringBuilder();
        code.append("// Using fetch API\n");
        code.append("const url = \"").append(url).append("\";\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("const options = {\n");
            code.append("  method: \"").append(method).append("\",\n");
            code.append("  headers: {\n");
            code.append("    \"Content-Type\": \"application/json\"\n");
            code.append("  },\n");
            code.append("  body: JSON.stringify({})\n");
            code.append("};\n\n");
        } else {
            code.append("const options = {\n");
            code.append("  method: \"").append(method).append("\"\n");
            code.append("};\n\n");
        }
        
        code.append("fetch(url, options)\n");
        code.append("  .then(response => response.json())\n");
        code.append("  .then(data => console.log(data))\n");
        code.append("  .catch(error => console.error('Error:', error));\n");
        
        return code.toString();
    }

    /**
     * Generate TypeScript code.
     *
     * @param url the API URL
     * @param method the HTTP method
     * @param operation the operation
     * @return the generated code
     */
    private String generateTypeScriptCode(String url, String method, Operation operation) {
        StringBuilder code = new StringBuilder();
        code.append("// Using fetch API with TypeScript\n");
        code.append("const url: string = \"").append(url).append("\";\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("const options: RequestInit = {\n");
            code.append("  method: \"").append(method).append("\",\n");
            code.append("  headers: {\n");
            code.append("    \"Content-Type\": \"application/json\"\n");
            code.append("  },\n");
            code.append("  body: JSON.stringify({})\n");
            code.append("};\n\n");
        } else {
            code.append("const options: RequestInit = {\n");
            code.append("  method: \"").append(method).append("\"\n");
            code.append("};\n\n");
        }
        
        code.append("async function callApi(): Promise<any> {\n");
        code.append("  try {\n");
        code.append("    const response: Response = await fetch(url, options);\n");
        code.append("    const data: any = await response.json();\n");
        code.append("    console.log(data);\n");
        code.append("    return data;\n");
        code.append("  } catch (error) {\n");
        code.append("    console.error('Error:', error);\n");
        code.append("  }\n");
        code.append("}\n\n");
        code.append("callApi();\n");
        
        return code.toString();
    }

    /**
     * Generate C# code.
     *
     * @param url the API URL
     * @param method the HTTP method
     * @param operation the operation
     * @return the generated code
     */
    private String generateCSharpCode(String url, String method, Operation operation) {
        StringBuilder code = new StringBuilder();
        code.append("using System;\n");
        code.append("using System.Net.Http;\n");
        code.append("using System.Text;\n");
        code.append("using System.Threading.Tasks;\n\n");
        
        code.append("class Program\n");
        code.append("{\n");
        code.append("    static async Task Main(string[] args)\n");
        code.append("    {\n");
        code.append("        using (HttpClient client = new HttpClient())\n");
        code.append("        {\n");
        code.append("            string url = \"").append(url).append("\";\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("            string json = \"{}\";\n");
            code.append("            StringContent content = new StringContent(json, Encoding.UTF8, \"application/json\");\n\n");
            
            if ("POST".equals(method)) {
                code.append("            HttpResponseMessage response = await client.PostAsync(url, content);\n");
            } else if ("PUT".equals(method)) {
                code.append("            HttpResponseMessage response = await client.PutAsync(url, content);\n");
            } else {
                code.append("            HttpRequestMessage request = new HttpRequestMessage(new HttpMethod(\"PATCH\"), url);\n");
                code.append("            request.Content = content;\n");
                code.append("            HttpResponseMessage response = await client.SendAsync(request);\n");
            }
        } else if ("DELETE".equals(method)) {
            code.append("            HttpResponseMessage response = await client.DeleteAsync(url);\n");
        } else {
            code.append("            HttpResponseMessage response = await client.GetAsync(url);\n");
        }
        
        code.append("            response.EnsureSuccessStatusCode();\n");
        code.append("            string responseBody = await response.Content.ReadAsStringAsync();\n");
        code.append("            Console.WriteLine(responseBody);\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }

    /**
     * Generate Go code.
     *
     * @param url the API URL
     * @param method the HTTP method
     * @param operation the operation
     * @return the generated code
     */
    private String generateGoCode(String url, String method, Operation operation) {
        StringBuilder code = new StringBuilder();
        code.append("package main\n\n");
        code.append("import (\n");
        code.append("    \"fmt\"\n");
        code.append("    \"io/ioutil\"\n");
        code.append("    \"net/http\"\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("    \"strings\"\n");
        }
        
        code.append(")\n\n");
        
        code.append("func main() {\n");
        code.append("    url := \"").append(url).append("\"\n");
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            code.append("    payload := \"{}\"\n");
            code.append("    req, err := http.NewRequest(\"").append(method).append("\", url, strings.NewReader(payload))\n");
            code.append("    if err != nil {\n");
            code.append("        fmt.Println(\"Error creating request:\", err)\n");
            code.append("        return\n");
            code.append("    }\n\n");
            code.append("    req.Header.Set(\"Content-Type\", \"application/json\")\n");
        } else {
            code.append("    req, err := http.NewRequest(\"").append(method).append("\", url, nil)\n");
            code.append("    if err != nil {\n");
            code.append("        fmt.Println(\"Error creating request:\", err)\n");
            code.append("        return\n");
            code.append("    }\n");
        }
        
        code.append("    client := &http.Client{}\n");
        code.append("    resp, err := client.Do(req)\n");
        code.append("    if err != nil {\n");
        code.append("        fmt.Println(\"Error sending request:\", err)\n");
        code.append("        return\n");
        code.append("    }\n");
        code.append("    defer resp.Body.Close()\n\n");
        
        code.append("    body, err := ioutil.ReadAll(resp.Body)\n");
        code.append("    if err != nil {\n");
        code.append("        fmt.Println(\"Error reading response:\", err)\n");
        code.append("        return\n");
        code.append("    }\n\n");
        
        code.append("    fmt.Println(\"Status:\", resp.Status)\n");
        code.append("    fmt.Println(\"Response:\", string(body))\n");
        code.append("}\n");
        
        return code.toString();
    }

    /**
     * Create or update a code sample.
     *
     * @param documentation the API documentation
     * @param operationId the operation ID
     * @param path the API path
     * @param method the HTTP method
     * @param language the programming language
     * @param code the code sample
     * @return the created or updated code sample
     */
    private CodeSample createOrUpdateCodeSample(ApiDocumentation documentation, String operationId, 
                                              String path, String method, ProgrammingLanguage language, String code) {
        Optional<CodeSample> existingSample = codeSampleRepository.findByDocumentationAndOperationIdAndLanguage(
                documentation, operationId, language);
        
        if (existingSample.isPresent()) {
            CodeSample sample = existingSample.get();
            sample.setCode(code);
            sample.setUpdatedAt(Instant.now());
            return codeSampleRepository.save(sample);
        } else {
            CodeSample sample = new CodeSample();
            sample.setDocumentation(documentation);
            sample.setOperationId(operationId);
            sample.setPath(path);
            sample.setMethod(method);
            sample.setLanguage(language);
            sample.setCode(code);
            sample.setCreatedAt(Instant.now());
            sample.setUpdatedAt(Instant.now());
            return codeSampleRepository.save(sample);
        }
    }

    /**
     * Generate client libraries for an API documentation.
     *
     * @param documentation the API documentation
     */
    private void generateClientLibraries(ApiDocumentation documentation) {
        String serviceName = documentation.getService().getName();
        String versionName = documentation.getVersion().getName();
        
        // Create directory for client libraries
        Path clientLibsDir = Paths.get(basePath, serviceName, versionName, "client-libs");
        try {
            Files.createDirectories(clientLibsDir);
        } catch (IOException e) {
            log.error("Error creating client libraries directory: {}", e.getMessage(), e);
            return;
        }
        
        // Save OpenAPI spec to a temporary file
        Path tempSpecPath;
        try {
            tempSpecPath = Files.createTempFile("openapi-", ".json");
            Files.writeString(tempSpecPath, documentation.getOpenApiSpec());
        } catch (IOException e) {
            log.error("Error creating temporary OpenAPI spec file: {}", e.getMessage(), e);
            return;
        }
        
        // Generate client libraries for each language
        for (String language : supportedLanguages) {
            try {
                generateClientLibrary(documentation, language, tempSpecPath.toString(), clientLibsDir.toString());
            } catch (Exception e) {
                log.error("Error generating client library for {}: {}", language, e.getMessage(), e);
            }
        }
        
        // Clean up temporary file
        try {
            Files.deleteIfExists(tempSpecPath);
        } catch (IOException e) {
            log.warn("Error deleting temporary OpenAPI spec file: {}", e.getMessage());
        }
    }

    /**
     * Generate a client library for a specific language.
     *
     * @param documentation the API documentation
     * @param language the programming language
     * @param specPath the path to the OpenAPI specification file
     * @param outputDir the output directory
     */
    private void generateClientLibrary(ApiDocumentation documentation, String language, String specPath, String outputDir) {
        String serviceName = documentation.getService().getName();
        
        // Map language to OpenAPI Generator language
        String generatorLanguage;
        switch (language.toLowerCase()) {
            case "java":
                generatorLanguage = "java";
                break;
            case "python":
                generatorLanguage = "python";
                break;
            case "javascript":
                generatorLanguage = "javascript";
                break;
            case "typescript":
                generatorLanguage = "typescript-fetch";
                break;
            case "csharp":
                generatorLanguage = "csharp";
                break;
            case "go":
                generatorLanguage = "go";
                break;
            default:
                log.warn("Unsupported language for client library generation: {}", language);
                return;
        }
        
        // Configure OpenAPI Generator
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setGeneratorName(generatorLanguage);
        configurator.setInputSpec(specPath);
        configurator.setOutputDir(outputDir + File.separator + language.toLowerCase());
        
        // Set additional properties
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("apiPackage", "com.codebridge." + serviceName.toLowerCase() + ".api");
        additionalProperties.put("modelPackage", "com.codebridge." + serviceName.toLowerCase() + ".model");
        additionalProperties.put("invokerPackage", "com.codebridge." + serviceName.toLowerCase() + ".client");
        additionalProperties.put("artifactId", serviceName.toLowerCase() + "-client");
        additionalProperties.put("artifactVersion", documentation.getVersion().getName());
        configurator.setAdditionalProperties(additionalProperties);
        
        // Generate client library
        try {
            final ClientOptInput clientOptInput = configurator.toClientOptInput();
            new DefaultGenerator().opts(clientOptInput).generate();
            log.info("Generated client library for {} in {}", language, outputDir + File.separator + language.toLowerCase());
        } catch (Exception e) {
            log.error("Error generating client library for {}: {}", language, e.getMessage(), e);
        }
    }

    /**
     * Get code samples for an API documentation.
     *
     * @param documentation the API documentation
     * @return the list of code samples
     */
    public List<CodeSample> getCodeSamples(ApiDocumentation documentation) {
        return codeSampleRepository.findByDocumentation(documentation);
    }

    /**
     * Get code samples for an API documentation and language.
     *
     * @param documentation the API documentation
     * @param language the programming language
     * @return the list of code samples
     */
    public List<CodeSample> getCodeSamplesByLanguage(ApiDocumentation documentation, ProgrammingLanguage language) {
        return codeSampleRepository.findByDocumentationAndLanguage(documentation, language);
    }

    /**
     * Get a code sample by ID.
     *
     * @param id the code sample ID
     * @return the code sample
     */
    public CodeSample getCodeSampleById(UUID id) {
        return codeSampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code sample not found with ID: " + id));
    }

    /**
     * Update a code sample.
     *
     * @param id the code sample ID
     * @param code the new code
     * @return the updated code sample
     */
    @Transactional
    public CodeSample updateCodeSample(UUID id, String code) {
        CodeSample sample = getCodeSampleById(id);
        sample.setCode(code);
        sample.setUpdatedAt(Instant.now());
        return codeSampleRepository.save(sample);
    }

    /**
     * Delete a code sample.
     *
     * @param id the code sample ID
     */
    @Transactional
    public void deleteCodeSample(UUID id) {
        codeSampleRepository.deleteById(id);
    }
}

