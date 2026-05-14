package com.connectsphere.post.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Inter-service HTTP client for the Search Service.
 * <p>
 * Responsible for synchronizing the central search index whenever a post
 * is created or deleted, ensuring real-time global search accuracy.
 * </p>
 *
 * <h3>Client Architecture</h3>
 * <pre class="mermaid">
 * graph LR;
 *     A[PostService] -->|index/remove| B[SearchServiceClient];
 *     B -->|HTTP POST/DELETE| C[Search Service API];
 * </pre>
 */
@Component
public class SearchServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceClient.class);

    private final RestClient restClient;
    private final String searchServiceBaseUrl;

    public SearchServiceClient(RestClient.Builder restClientBuilder,
                               @Value("${connectsphere.search-service.base-url:http://localhost:8088/search}") String searchServiceBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.searchServiceBaseUrl = searchServiceBaseUrl;
    }

    public void indexPost(Long postId, String content) {
        restClient.post()
                .uri(searchServiceBaseUrl + "/index")
                .body(Map.of(
                        "postId", postId,
                        "content", content == null ? "" : content
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Search service returned " + response.getStatusCode());
                })
                .toBodilessEntity();

        logger.info("Indexed post {} in search service", postId);
    }

    public void removePostIndex(Long postId) {
        restClient.delete()
                .uri(searchServiceBaseUrl + "/index/{postId}", postId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Search service returned " + response.getStatusCode());
                })
                .toBodilessEntity();

        logger.info("Removed post {} from search index", postId);
    }
}
