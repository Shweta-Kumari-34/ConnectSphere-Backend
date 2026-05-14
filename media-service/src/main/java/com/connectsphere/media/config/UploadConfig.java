package com.connectsphere.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for handling multipart file uploads in the Media Service.
 * <p>
 * This class ensures that uploaded files meet strict validation criteria
 * (size limits and allowed MIME types for images/videos). It also registers
 * a resource handler so the frontend can retrieve uploaded media statically.
 * </p>
 *
 * <h3>Upload Configuration Context</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[Frontend Upload] --> B[MediaController];
 *     B --> C[UploadConfig Validator];
 *     C -->|Invalid Size/Type| D[BadRequestException];
 *     C -->|Valid| E[Save to Local Directory];
 *     E --> F[Serve via /uploads/**];
 * </pre>
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Value("${connectsphere.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${connectsphere.upload.max-image-size-kb:10240}")
    private long maxImageSizeKb;

    @Value("${connectsphere.upload.max-video-size-kb:51200}")
    private long maxVideoSizeKb;

    @Value("${connectsphere.upload.allowed-image-types:image/jpeg,image/png,image/webp}")
    private String allowedImageTypes;

    @Value("${connectsphere.upload.allowed-video-types:video/mp4}")
    private String allowedVideoTypes;

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
    }

    public String getUploadDir() { return uploadDir; }
    public long getMaxImageSizeKb() { return maxImageSizeKb; }
    public long getMaxVideoSizeKb() { return maxVideoSizeKb; }

    public List<String> getAllowedImageTypes() {
        return Arrays.asList(allowedImageTypes.split(","));
    }

    public List<String> getAllowedVideoTypes() {
        return Arrays.asList(allowedVideoTypes.split(","));
    }

    public boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        return getAllowedImageTypes().contains(contentType) || getAllowedVideoTypes().contains(contentType);
    }

    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    public long getMaxSizeKb(String contentType) {
        return isImage(contentType) ? maxImageSizeKb : maxVideoSizeKb;
    }

    /** Serve /uploads/** as static resources so frontend can display them */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = new File(uploadDir).getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absPath + "/");
    }
}
