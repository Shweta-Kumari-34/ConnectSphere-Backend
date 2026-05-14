package com.connectsphere.follow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 * <p>
 * Exposes the API specifications for the Follow Service, detailing endpoints
 * for following, unfollowing, fetching followers, and listing following users.
 * </p>
 *
 * <h3>API Documentation Context</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[API Clients / Frontend] -->|Access Docs| B[Swagger UI];
 *     B --> C[Follow Controllers];
 * </pre>
 */
@Configuration
public class SwaggerConfig {

    /**
     * Defines the base OpenAPI configuration for the Follow Service.
     * 
     * @return the configured {@link OpenAPI} object
     */
    @Bean
    public OpenAPI followServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Follow Service API")
                        .description("Follow, unfollow, followers, and following APIs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Team")
                                .email("admin@connectsphere.com")));
    }
}
