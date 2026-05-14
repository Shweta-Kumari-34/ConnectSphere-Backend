package com.connectsphere.post.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web configuration for serving static media assets.
 * <p>
 * Maps the physical local "uploads" directory to the logical HTTP URL paths
 * so the frontend can retrieve video thumbnails and reel media.
 * </p>
 *
 * <h3>Static Resource Mapping</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[Frontend Request] -->|/uploads/reels/*| B[WebMvcConfig];
 *     B -->|Serve File| C[Local File System];
 * </pre>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory("uploads", registry);
    }

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toUri().toString();

        registry.addResourceHandler("/" + dirName + "/**")
                .addResourceLocations(uploadPath);
    }
}
