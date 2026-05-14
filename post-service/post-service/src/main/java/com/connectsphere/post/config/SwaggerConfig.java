package com.connectsphere.post.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * SwaggerConfig
 * -------------
 * Access Swagger UI at: http://localhost:8082/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI postServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Post Service API")
                        .description("Post Management APIs — create, list, and manage social media posts")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Team")
                                .email("admin@connectsphere.com")));
    }
}
