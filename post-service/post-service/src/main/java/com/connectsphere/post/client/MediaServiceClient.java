package com.connectsphere.post.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Inter-service HTTP client for the Media Service.
 * <p>
 * Ensures that when a post is soft-deleted, any associated media files
 * (images/videos) in the Media Service are also flagged for cleanup.
 * </p>
 *
 * <h3>Client Architecture</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[PostService] -->|delete| B[MediaServiceClient];
 *     B -->|HTTP DELETE| C[Media Service API];
 * </pre>
 */
@Component
public class MediaServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MediaServiceClient.class);

    private final RestClient restClient;
    private final String mediaServiceBaseUrl;

    public MediaServiceClient(RestClient.Builder restClientBuilder,
                              @Value("${connectsphere.media-service.base-url:http://localhost:8092/media}") String mediaServiceBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.mediaServiceBaseUrl = mediaServiceBaseUrl;
    }

    public void softDeleteMediaByPost(Long postId) {
        restClient.delete()
                .uri(mediaServiceBaseUrl + "/post/{postId}", postId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Media service returned " + response.getStatusCode());
                })
                .toBodilessEntity();

        logger.info("Soft-deleted media for post {}", postId);
    }
}
