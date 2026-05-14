package com.connectsphere.post.service.impl;

import com.connectsphere.post.client.MediaServiceClient;
import com.connectsphere.post.client.SearchServiceClient;
import com.connectsphere.post.dto.NotificationEvent;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.kafka.PostEventProducer;
import com.connectsphere.post.producer.NotificationEventProducer;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.service.PostService;
import com.connectsphere.post.util.PostConstants;
import com.connectsphere.post.exception.ResourceNotFoundException;
import com.connectsphere.post.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h1>PostServiceImpl</h1>
 * <p>Implementation of {@link PostService} responsible for managing user posts, feed generation,
 * and social interactions (likes, comments, mentions).</p>
 * 
 * <h2>Post Creation & Notification Pipeline:</h2>
 * <pre>
 * sequenceDiagram
 *     User->>PostService: createPost(Content)
 *     PostService->>DB: Save Post Entity
 *     PostService->>Kafka: Publish PostCreated Event
 *     PostService->>SearchService: Index Post Content
 *     Note over PostService: Extract @mentions via Regex
 *     PostService->>RabbitMQ: Dispatch Mention Notifications
 *     RabbitMQ-->>NotificationService: Alert Users
 * </pre>
 * 
 * <h2>Key Logic Features:</h2>
 * <ul>
 *     <li><b>Soft-Delete Mechanism:</b> Complies with data retention policies while hiding content.</li>
 *     <li><b>Asynchronous Processing:</b> Offloads analytics and notifications to Kafka/RabbitMQ.</li>
 *     <li><b>Cross-Service Orchestration:</b> Coordinates with {@code MediaService} for asset cleanup and {@code SearchService} for indexing.</li>
 *     <li><b>Regex Mention Extraction:</b> Dynamically parses post bodies to alert tagged users in real time.</li>
 * </ul>
 */
@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final MediaServiceClient mediaServiceClient;
    private final SearchServiceClient searchServiceClient;
    private final NotificationEventProducer notificationEventProducer;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PostEventProducer postEventProducer;

    public PostServiceImpl(PostRepository postRepository,
                           MediaServiceClient mediaServiceClient,
                           SearchServiceClient searchServiceClient,
                           NotificationEventProducer notificationEventProducer) {
        this.postRepository = postRepository;
        this.mediaServiceClient = mediaServiceClient;
        this.searchServiceClient = searchServiceClient;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    public Post createPost(Post post) {
        post.setDeleted(false);
        Post savedPost = postRepository.save(post);

        // Publish Kafka event (non-blocking)
        if (postEventProducer != null) {
            try {
                postEventProducer.sendPostCreatedEvent(savedPost.getUserEmail(), savedPost.getTitle());
            } catch (Exception e) {
                log.warn("Kafka event publish failed (Kafka may not be running): {}", e.getMessage());
            }
        }
        syncSearchIndex(savedPost);
        publishMentionNotifications(savedPost);

        return savedPost;
    }

    @Override
    public Post updatePost(Long id, Post updatedPost, String userEmail) {
        Post existing = getActivePostById(id);
        if (!existing.getUserEmail().equals(userEmail)) {
            throw new UnauthorizedException("You can only update your own posts");
        }
        existing.setTitle(updatedPost.getTitle());
        existing.setContent(updatedPost.getContent());
        if (updatedPost.getPostType() != null && !updatedPost.getPostType().isBlank()) {
            existing.setPostType(updatedPost.getPostType());
        }
        if (updatedPost.getVisibility() != null && !updatedPost.getVisibility().isBlank()) {
            existing.setVisibility(updatedPost.getVisibility());
        }
        if (updatedPost.getMediaUrls() != null) {
            existing.setMediaUrls(updatedPost.getMediaUrls());
        }
        Post savedPost = postRepository.save(existing);
        refreshSearchIndex(savedPost);
        return savedPost;
    }

    @Override
    public void deletePost(Long id, String userEmail, String userRole) {
        Post existing = getActivePostById(id);
        boolean isAdmin = userRole != null
                && userRole.trim().toUpperCase().contains(PostConstants.ROLE_ADMIN);
        if (!existing.getUserEmail().equals(userEmail) && !isAdmin) {
            throw new UnauthorizedException("You can only delete your own posts");
        }
        existing.setDeleted(true); // soft-delete per case study
        postRepository.save(existing);
        try {
            searchServiceClient.removePostIndex(id);
        } catch (Exception e) {
            log.warn("Post {} was soft-deleted but search index cleanup failed: {}", id, e.getMessage());
        }
        try {
            mediaServiceClient.softDeleteMediaByPost(id);
        } catch (Exception e) {
            log.warn("Post {} was soft-deleted but media cleanup failed: {}", id, e.getMessage());
        }
    }

    @Override
    public Post getPostById(Long id) {
        return getActivePostById(id);
    }

    @Override
    public List<Post> getPostsByUser(String userEmail) {
        return postRepository.findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc(userEmail);
    }

    @Override
    public List<Post> getFeed() {
        return postRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @Override
    public List<Post> searchPosts(String keyword) {
        return postRepository.searchByContent(keyword);
    }

    @Override
    public Post updateVisibility(Long id, String visibility, String userEmail) {
        Post existing = getActivePostById(id);
        if (!existing.getUserEmail().equals(userEmail)) {
            throw new UnauthorizedException("You can only change visibility of your own posts");
        }
        existing.setVisibility(visibility);
        return postRepository.save(existing);
    }

    @Override
    public void incrementLikes(Long postId) {
        Post post = getPostById(postId);
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementLikes(Long postId) {
        Post post = getPostById(postId);
        if (post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
        }
        postRepository.save(post);
    }

    @Override
    public void incrementComments(Long postId) {
        Post post = getPostById(postId);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementComments(Long postId) {
        Post post = getPostById(postId);
        if (post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
        }
        postRepository.save(post);
    }

    @Override
    public long getPostCount(String userEmail) {
        return postRepository.countByUserEmailAndIsDeletedFalse(userEmail);
    }

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc();
    }

    private Post getActivePostById(Long id) {
        return postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    private void publishMentionNotifications(Post post) {
        String content = post.getContent() == null ? "" : post.getContent();
        Set<String> mentionedEmails = extractMentionedEmails(content);
        for (String mentionedEmail : mentionedEmails) {
            if (mentionedEmail.equalsIgnoreCase(post.getUserEmail())) {
                continue;
            }
            try {
                NotificationEvent event = new NotificationEvent();
                event.setRecipientEmail(mentionedEmail);
                event.setActorEmail(post.getUserEmail());
                event.setType(PostConstants.NOTIFICATION_TYPE_MENTION);
                event.setTargetId(post.getId());
                event.setDeepLinkUrl(PostConstants.DEEP_LINK_POST + post.getId());
                event.setMessage(post.getUserEmail().split("@")[0] + " mentioned you in a post");
                event.setPriority(PostConstants.NOTIFICATION_PRIORITY_NORMAL);
                event.setCreatedAt(java.time.LocalDateTime.now());
                notificationEventProducer.publish(event);
            } catch (Exception e) {
                log.debug("Unable to publish mention notification for {}", mentionedEmail);
            }
        }
    }

    private Set<String> extractMentionedEmails(String content) {
        Pattern pattern = Pattern.compile("@([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
        Matcher matcher = pattern.matcher(content);
        return matcher.results().map(r -> r.group(1)).collect(Collectors.toSet());
    }

    private void syncSearchIndex(Post post) {
        try {
            searchServiceClient.indexPost(post.getId(), post.getContent());
        } catch (Exception e) {
            log.warn("Search indexing failed for post {}: {}", post.getId(), e.getMessage());
        }
    }

    private void refreshSearchIndex(Post post) {
        try {
            searchServiceClient.removePostIndex(post.getId());
            searchServiceClient.indexPost(post.getId(), post.getContent());
        } catch (Exception e) {
            log.warn("Search reindex failed for post {}: {}", post.getId(), e.getMessage());
        }
    }
}
