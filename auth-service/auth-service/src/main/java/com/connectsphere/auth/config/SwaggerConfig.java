package com.connectsphere.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 * <p>
 * This class exposes the API specifications for the Auth Service, making it
 * easy for frontend developers and other services to understand the available
 * endpoints, request structures, and response formats.
 * </p>
 *
 * <h3>API Documentation</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Developer/Client] -->|Access UI| B[Swagger UI];
 *     A -->|Access JSON Docs| C[OpenAPI Docs];
 *     B --> C;
 *     C --> D[Auth Service Controllers];
 * </pre>
 *
 * Access Swagger UI at: {@code http://localhost:8081/swagger-ui.html}<br>
 * Access API docs at: {@code http://localhost:8081/v3/api-docs}
 */
@Configuration
public class SwaggerConfig {

    /**
     * Defines the base OpenAPI configuration for the Auth Service.
     * 
     * @return the configured {@link OpenAPI} object
     */
    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Auth Service API")
                        .description("Authentication & User Management APIs — register, login, change password, update profile")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Team")
                                .email("admin@connectsphere.com")));
    }
}
