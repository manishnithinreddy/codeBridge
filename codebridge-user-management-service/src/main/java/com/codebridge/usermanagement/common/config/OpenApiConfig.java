package com.codebridge.usermanagement.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for OpenAPI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * Configures the OpenAPI documentation.
     *
     * @return the OpenAPI configuration
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeBridge User Management API")
                        .description("API for managing users, authentication, sessions, teams, and application settings")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CodeBridge Team")
                                .email("support@codebridge.com")
                                .url("https://codebridge.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(getServers())
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    /**
     * Gets the servers for the OpenAPI documentation.
     *
     * @return the list of servers
     */
    private List<Server> getServers() {
        Server localServer = new Server();
        localServer.setUrl(contextPath);
        localServer.setDescription("Local Server");

        Server devServer = new Server();
        devServer.setUrl("https://dev-api.codebridge.com" + contextPath);
        devServer.setDescription("Development Server");

        Server stagingServer = new Server();
        stagingServer.setUrl("https://staging-api.codebridge.com" + contextPath);
        stagingServer.setDescription("Staging Server");

        Server productionServer = new Server();
        productionServer.setUrl("https://api.codebridge.com" + contextPath);
        productionServer.setDescription("Production Server");

        return Arrays.asList(localServer, devServer, stagingServer, productionServer);
    }
}

