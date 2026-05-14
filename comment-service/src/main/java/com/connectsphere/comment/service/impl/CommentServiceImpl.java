package com.connectsphere.comment.service.impl;

import com.connectsphere.comment.dto.CommentRequestDto;
import com.connectsphere.comment.dto.NotificationEvent;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.producer.NotificationEventProducer;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.service.CommentService;
import com.connectsphere.comment.util.CommentConstants;
import com.connectsphere.comment.exception.ResourceNotFoundException;
import com.connectsphere.comment.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <h1>CommentServiceImpl</h1>
 * <p>Implementation of {@link CommentService} that manages the lifecycle of social comments 
 * and orchestrates cross-service notifications.</p>
 * 
 * <h2>Notification Pipeline:</h2>
 * <pre>
 * sequenceDiagram
 *     Actor->>CommentService: addComment()
 *     CommentService->>DB: Save Comment
 *     CommentService->>RestTemplate: Fetch Content Metadata (Post/Reel)
 *     CommentService->>Kafka: Publish NotificationEvent
 *     Note over Kafka: Async delivery to Notification Service
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Ownership Verification:</b> Ensures only the author can update or delete their comments.</li>
 *     <li><b>Cross-Service Integration:</b> Uses {@link RestTemplate} to verify existence and authorship of content (Posts/Reels).</li>
 *     <li><b>Async Eventing:</b> Publishes to {@link NotificationEventProducer} to minimize latency on comment creation.</li>
 *     <li><b>Hierarchical Data:</b> Handles flat storage with logical parent-child linking for replies.</li>
 * </ul>
 */
@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;
    private final NotificationEventProducer notificationEventProducer;

    public CommentServiceImpl(CommentRepository commentRepository, 
                              RestTemplate restTemplate,
                              NotificationEventProducer notificationEventProducer) {
        this.commentRepository = commentRepository;
        this.restTemplate = restTemplate;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    public Comment addComment(String userEmail, CommentRequestDto request) {
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setParentId(request.getParentId());
        comment.setUserEmail(userEmail);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setDeleted(false);
        comment.setCreatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        sendCommentNotification(saved);
        return saved;
    }

    @Override
    public List<Comment> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }

    @Override
    public List<Comment> getReplies(Long parentId) {
        return commentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId);
    }

    @Override
    public Comment getCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
    }

    @Override
    public Comment updateComment(Long id, String userEmail, String content) {
        Comment comment = getCommentById(id);
        if (!comment.getUserEmail().equals(userEmail)) {
            throw new UnauthorizedException("Not authorized to update this comment");
        }
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Override
    public void deleteComment(Long id, String userEmail) {
        Comment comment = getCommentById(id);
        if (!comment.getUserEmail().equals(userEmail)) {
            throw new UnauthorizedException("Not authorized to delete this comment");
        }
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    @Override
    public List<Comment> getCommentsByUser(String userEmail) {
        return commentRepository.findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc(userEmail);
    }

    @Override
    public void likeComment(Long id) {
        Comment comment = getCommentById(id);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    public void unlikeComment(Long id) {
        Comment comment = getCommentById(id);
        if (comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
        }
        commentRepository.save(comment);
    }

    @Override
    public long getCommentCount(Long postId) {
        return commentRepository.countByPostIdAndIsDeletedFalse(postId);
    }

    private void sendCommentNotification(Comment comment) {
        String actorEmail = comment.getUserEmail();
        if (actorEmail == null) return;

        String recipientEmail = null;
        String type = CommentConstants.NOTIFICATION_TYPE_COMMENT;
        String message = actorEmail.split("@")[0] + " commented on your post";
        Long referenceId = comment.getPostId();
        String referenceType = CommentConstants.REF_TYPE_POST;
        String deepLinkUrl = CommentConstants.DEEP_LINK_POST + comment.getPostId();

        if (comment.getParentId() != null) {
            Comment parent = commentRepository.findById(comment.getParentId()).orElse(null);
            if (parent != null) {
                recipientEmail = parent.getUserEmail();
                type = CommentConstants.NOTIFICATION_TYPE_REPLY;
                message = actorEmail.split("@")[0] + " replied to your comment";
                referenceId = parent.getId();
                // Even for replies, the overall referenceType for the thumbnail should be the Post/Reel
                // but the deep link might need to highlight the comment.
            }
        } else {
            // First try to find the recipient from the Post service
            try {
                Map<String, Object> post = restTemplate.getForObject(CommentConstants.POST_SERVICE_URL, Map.class, comment.getPostId());
                if (post != null && post.get("userEmail") != null) {
                    recipientEmail = String.valueOf(post.get("userEmail"));
                }
            } catch (Exception ignored) {
                // If not found in posts, try reels
                try {
                    Map<String, Object> reel = restTemplate.getForObject(CommentConstants.REEL_SERVICE_URL, Map.class, comment.getPostId());
                    if (reel != null && reel.get("userEmail") != null) {
                        recipientEmail = String.valueOf(reel.get("userEmail"));
                        referenceType = CommentConstants.REF_TYPE_REEL;
                        message = actorEmail.split("@")[0] + " commented on your reel";
                        deepLinkUrl = CommentConstants.DEEP_LINK_REEL + comment.getPostId();
                    }
                } catch (Exception ignored2) {}
            }
        }

        if (recipientEmail == null || recipientEmail.equalsIgnoreCase(actorEmail)) {
            return;
        }

        try {
            // Fetch metadata to include thumbnail preview in the notification
            String metadataJson = null;
            try {
                // Try post service first
                Map<String, Object> content = null;
                try {
                    content = restTemplate.getForObject(CommentConstants.POST_SERVICE_URL, Map.class, comment.getPostId());
                } catch (Exception ignored) {
                    // Try reel service
                    content = restTemplate.getForObject(CommentConstants.REEL_SERVICE_URL, Map.class, comment.getPostId());
                    if (content != null) {
                        referenceType = CommentConstants.REF_TYPE_REEL;
                        if (comment.getParentId() == null) {
                            message = actorEmail.split("@")[0] + " commented on your reel";
                            deepLinkUrl = CommentConstants.DEEP_LINK_REEL + comment.getPostId();
                        } else {
                            deepLinkUrl = CommentConstants.DEEP_LINK_REEL + comment.getPostId() + "&commentId=" + comment.getId();
                        }
                    }
                }

                if (content != null) {
                    String mediaUrl = asString(content.get("mediaUrl"));
                    if (mediaUrl == null) mediaUrl = asString(content.get("imageUrl"));
                    String caption = asString(content.get("caption"));
                    
                    String safeImage = mediaUrl == null ? "null" : "\"" + mediaUrl.replace("\"", "\\\"") + "\"";
                    String safeCaption = caption == null ? "null" : "\"" + caption.replace("\"", "\\\"") + "\"";
                    
                    metadataJson = String.format(
                            "{\"mediaUrl\":%s,\"caption\":%s,\"referenceType\":\"%s\",\"postId\":%d}",
                            safeImage, safeCaption, referenceType, comment.getPostId());
                }
            } catch (Exception ignored) {}

            NotificationEvent event = new NotificationEvent();
            event.setRecipientEmail(recipientEmail);
            event.setActorEmail(actorEmail);
            event.setType(type);
            event.setTargetId(referenceId);
            event.setDeepLinkUrl(deepLinkUrl);
            event.setMessage(message);
            event.setPriority(CommentConstants.NOTIFICATION_PRIORITY_NORMAL);
            event.setMetadata(metadataJson);
            event.setCreatedAt(LocalDateTime.now());
            notificationEventProducer.publish(event);
        } catch (Exception ignored) {}
    }

    /** Safely converts any Object to a trimmed String, returns null for blank/null values. */
    private String asString(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
