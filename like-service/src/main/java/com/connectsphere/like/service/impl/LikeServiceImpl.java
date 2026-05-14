package com.connectsphere.like.service.impl;

import com.connectsphere.like.entity.LikeEntity;
import com.connectsphere.like.dto.NotificationEvent;
import com.connectsphere.like.producer.NotificationEventProducer;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.service.LikeService;
import com.connectsphere.like.util.LikeConstants;
import com.connectsphere.like.exception.ResourceNotFoundException;
import com.connectsphere.like.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>LikeServiceImpl</h1>
 * <p>Implementation of {@link LikeService} that handles cross-service engagement tracking 
 * and asynchronous notification dispatching.</p>
 * 
 * <h2>Owner Resolution & Notification Flow:</h2>
 * <pre>
 * sequenceDiagram
 *     Actor->>LikeService: likeTarget(ID, Type)
 *     LikeService->>DB: Persist Reaction
 *     LikeService->>RestTemplate: Resolve Owner (Post/Reel/Comment/Story)
 *     LikeService->>Kafka: Publish NotificationEvent
 *     Note over Kafka: Includes rich metadata (captions, thumbnails)
 * </pre>
 * 
 * <h2>Key Logic Features:</h2>
 * <ul>
 *     <li><b>Multi-Target Support:</b> Dynamically resolves recipients across four different microservices via {@link RestTemplate}.</li>
 *     <li><b>Cache Consistency:</b> Extensive use of Spring Cache with complex eviction keys for high-performance reads.</li>
 *     <li><b>Story Interaction Logic:</b> Allows idempotent reaction updates for stories instead of blocking duplicates.</li>
 *     <li><b>Rich Events:</b> Fetches content thumbnails (Post images, Reel media) to provide premium "Instagram-style" notifications.</li>
 * </ul>
 */
@Service
public class LikeServiceImpl implements LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    private final LikeRepository likeRepository;
    private final RestTemplate restTemplate;
    private final NotificationEventProducer notificationEventProducer;

    public LikeServiceImpl(LikeRepository likeRepository, 
                           RestTemplate restTemplate,
                           NotificationEventProducer notificationEventProducer) {
        this.likeRepository = likeRepository;
        this.restTemplate = restTemplate;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "likeHasLiked", key = "#a0 + '|' + #a1 + '|' + #a2"),
            @CacheEvict(value = "likeByTarget", key = "#a1 + '|' + #a2"),
            @CacheEvict(value = "likeByUser", key = "#a0"),
            @CacheEvict(value = "likeCount", key = "#a1 + '|' + #a2"),
            @CacheEvict(value = "likeCountByType", allEntries = true),
            @CacheEvict(value = "likeReactionSummary", key = "#a1 + '|' + #a2")
    })
    public LikeEntity likeTarget(String userEmail, Long targetId, String targetType, String reactionType) {
        String normalizedType = targetType == null ? "" : targetType.trim().toUpperCase();
        
        // For stories, we allow changing the reaction (emoji) instead of blocking with "Already liked"
        if ("STORY".equals(normalizedType)) {
            LikeEntity existing = likeRepository.findByTargetIdAndTargetTypeAndUserEmail(targetId, targetType, userEmail).orElse(null);
            if (existing != null) {
                existing.setReactionType(reactionType != null ? reactionType : "LIKE");
                existing.setCreatedAt(LocalDateTime.now());
                LikeEntity updated = likeRepository.save(existing);
                sendLikeNotification(updated);
                return updated;
            }
        } else {
            if (likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(targetId, targetType, userEmail)) {
                throw new ConflictException("Already liked");
            }
        }

        LikeEntity like = new LikeEntity();
        like.setTargetId(targetId);
        like.setTargetType(targetType);
        like.setUserEmail(userEmail);
        like.setReactionType(reactionType != null ? reactionType : "LIKE");
        like.setCreatedAt(LocalDateTime.now());
        LikeEntity saved = likeRepository.save(like);
        sendLikeNotification(saved);
        return saved;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "likeHasLiked", key = "#a0 + '|' + #a1 + '|' + #a2"),
            @CacheEvict(value = "likeByTarget", key = "#a1 + '|' + #a2"),
            @CacheEvict(value = "likeByUser", key = "#a0"),
            @CacheEvict(value = "likeCount", key = "#a1 + '|' + #a2"),
            @CacheEvict(value = "likeCountByType", allEntries = true),
            @CacheEvict(value = "likeReactionSummary", key = "#a1 + '|' + #a2")
    })
    public void unlikeTarget(String userEmail, Long targetId, String targetType) {
        LikeEntity like = likeRepository.findByTargetIdAndTargetTypeAndUserEmail(targetId, targetType, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));
        likeRepository.delete(like);
    }

    @Override
    @Cacheable(value = "likeHasLiked", key = "#a0 + '|' + #a1 + '|' + #a2")
    public boolean hasLiked(String userEmail, Long targetId, String targetType) {
        return likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(targetId, targetType, userEmail);
    }

    @Override
    @Cacheable(value = "likeByTarget", key = "#a0 + '|' + #a1", unless = "#result == null")
    public List<LikeEntity> getLikesByTarget(Long targetId, String targetType) {
        return likeRepository.findByTargetIdAndTargetType(targetId, targetType);
    }

    @Override
    @Cacheable(value = "likeByUser", key = "#a0", unless = "#result == null")
    public List<LikeEntity> getLikesByUser(String userEmail) {
        return likeRepository.findByUserEmail(userEmail);
    }

    @Override
    @Cacheable(value = "likeCount", key = "#a0 + '|' + #a1")
    public long getLikeCount(Long targetId, String targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "likeByTarget", key = "#a1 + '|' + #a2"),
            @CacheEvict(value = "likeByUser", key = "#a0"),
            @CacheEvict(value = "likeCountByType", allEntries = true),
            @CacheEvict(value = "likeReactionSummary", key = "#a1 + '|' + #a2")
    })
    public LikeEntity changeReaction(String userEmail, Long targetId, String targetType, String newReaction) {
        LikeEntity like = likeRepository.findByTargetIdAndTargetTypeAndUserEmail(targetId, targetType, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));
        like.setReactionType(newReaction);
        return likeRepository.save(like);
    }

    @Override
    @Cacheable(value = "likeCountByType", key = "#a0 + '|' + #a1 + '|' + #a2")
    public long getLikeCountByType(Long targetId, String targetType, String reactionType) {
        return likeRepository.countByTargetIdAndTargetTypeAndReactionType(targetId, targetType, reactionType);
    }

    @Override
    @Cacheable(value = "likeReactionSummary", key = "#a0 + '|' + #a1", unless = "#result == null")
    public Map<String, Long> getReactionSummary(Long targetId, String targetType) {
        List<LikeEntity> likes = likeRepository.findByTargetIdAndTargetType(targetId, targetType);
        Map<String, Long> summary = new HashMap<>();
        for (LikeEntity like : likes) {
            summary.merge(like.getReactionType(), 1L, Long::sum);
        }
        return summary;
    }

    private void sendLikeNotification(LikeEntity like) {
        String recipientEmail = resolveRecipientEmail(like.getTargetId(), like.getTargetType());
        String actorEmail = like.getUserEmail();
        if (recipientEmail == null || actorEmail == null || recipientEmail.equalsIgnoreCase(actorEmail)) {
            return;
        }

        String normalizedType = like.getTargetType() == null ? "" : like.getTargetType().trim().toUpperCase();
        String notificationType = LikeConstants.NOTIFICATION_TYPE_POST_LIKE;
        String actionUrl = LikeConstants.DEEP_LINK_POST + like.getTargetId();
        String referenceType = normalizedType.isEmpty() ? LikeConstants.TARGET_TYPE_POST : normalizedType;
        String metadataJson = null;

        if (LikeConstants.TARGET_TYPE_REEL.equals(normalizedType)) {
            notificationType = LikeConstants.NOTIFICATION_TYPE_REEL_LIKE;
            actionUrl = LikeConstants.DEEP_LINK_REEL + like.getTargetId();
        } else if (LikeConstants.TARGET_TYPE_STORY.equals(normalizedType)) {
            notificationType = LikeConstants.NOTIFICATION_TYPE_STORY_LIKE;
            actionUrl = LikeConstants.DEEP_LINK_STORY + like.getTargetId();
        } else if (LikeConstants.TARGET_TYPE_COMMENT.equals(normalizedType)) {
            notificationType = LikeConstants.NOTIFICATION_TYPE_COMMENT_LIKE;
            Long postId = resolveCommentPostId(like.getTargetId());
            actionUrl = postId != null ? (LikeConstants.DEEP_LINK_POST + postId + "&commentId=" + like.getTargetId()) : "/posts";
        }

        try {
            // Best-effort metadata for previews (safe if unavailable).
            if (LikeConstants.TARGET_TYPE_REEL.equals(normalizedType)) {
                try {
                    Map<String, Object> reel = restTemplate.getForObject(LikeConstants.REEL_SERVICE_URL, Map.class, like.getTargetId());
                    if (reel != null) {
                        String mediaUrl = asString(reel.get("mediaUrl"));
                        String caption = asString(reel.get("caption"));
                        metadataJson = String.format("{\"mediaUrl\":%s,\"caption\":%s}",
                                mediaUrl == null ? "null" : "\"" + mediaUrl.replace("\"", "\\\"") + "\"",
                                caption == null ? "null" : "\"" + caption.replace("\"", "\\\"") + "\"");
                    }
                } catch (Exception ignored) {}
            } else if (LikeConstants.TARGET_TYPE_STORY.equals(normalizedType)) {
                try {
                    Map<String, Object> story = restTemplate.getForObject(LikeConstants.STORY_SERVICE_URL, Map.class, like.getTargetId());
                    if (story != null) {
                        String mediaUrl = asString(story.get("mediaUrl"));
                        if (mediaUrl == null) {
                            mediaUrl = asString(story.get("media_url"));
                        }
                        metadataJson = mediaUrl == null ? null
                                : String.format("{\"mediaUrl\":\"%s\",\"referenceType\":\"STORY\"}", mediaUrl.replace("\"", "\\\""));
                    }
                } catch (Exception ignored) {}
            } else if (LikeConstants.TARGET_TYPE_POST.equals(normalizedType) || normalizedType.isEmpty()) {
                // Fetch post imageUrl + caption so the notification card can show
                // the actual post thumbnail next to "liked your post" (Instagram-style UX)
                try {
                    Map<String, Object> post = restTemplate.getForObject(LikeConstants.POST_SERVICE_URL, Map.class, like.getTargetId());
                    if (post != null) {
                        String imageUrl = asString(post.get("imageUrl"));
                        String caption  = asString(post.get("caption"));
                        String safeImg = imageUrl == null ? "null" : "\"" + imageUrl.replace("\"", "\\\"") + "\"";
                        String safeCap = caption  == null ? "null" : "\"" + caption.replace("\"", "\\\"")  + "\"";
                        metadataJson = String.format(
                                "{\"mediaUrl\":%s,\"caption\":%s,\"referenceType\":\"POST\"}", safeImg, safeCap);
                    }
                } catch (Exception ignored) {}
            }

            NotificationEvent event = new NotificationEvent();
            event.setRecipientEmail(recipientEmail);
            event.setActorEmail(actorEmail);
            event.setType(notificationType);
            event.setTargetId(like.getTargetId());
            event.setDeepLinkUrl(actionUrl);
            String verb = (like.getReactionType() == null || LikeConstants.REACTION_LIKE.equalsIgnoreCase(like.getReactionType())) 
                    ? "liked" : "reacted " + like.getReactionType() + " to";
            event.setMessage(actorEmail.split("@")[0] + " " + verb + " your " + referenceType.toLowerCase());
            event.setPriority(LikeConstants.NOTIFICATION_PRIORITY_NORMAL);
            event.setMetadata(metadataJson);
            event.setCreatedAt(LocalDateTime.now());
            notificationEventProducer.publish(event);
        } catch (Exception ignored) {}
    }

    private String resolveRecipientEmail(Long targetId, String targetType) {
        if (targetId == null || targetType == null) {
            return null;
        }
        String normalizedType = targetType.trim().toUpperCase();
        try {
            if (LikeConstants.TARGET_TYPE_REEL.equals(normalizedType)) {
                Map<String, Object> reel = restTemplate.getForObject(LikeConstants.REEL_SERVICE_URL, Map.class, targetId);
                return reel == null ? null : asString(reel.get("userEmail"));
            }
            if (LikeConstants.TARGET_TYPE_POST.equals(normalizedType)) {
                Map<String, Object> post = restTemplate.getForObject(LikeConstants.POST_SERVICE_URL, Map.class, targetId);
                return post == null ? null : asString(post.get("userEmail"));
            }
            if (LikeConstants.TARGET_TYPE_COMMENT.equals(normalizedType)) {
                Map<String, Object> comment = restTemplate.getForObject(LikeConstants.COMMENT_SERVICE_URL, Map.class, targetId);
                return comment == null ? null : asString(comment.get("userEmail"));
            }
            if (LikeConstants.TARGET_TYPE_STORY.equals(normalizedType)) {
                Map<String, Object> story = restTemplate.getForObject(LikeConstants.STORY_SERVICE_URL, Map.class, targetId);
                return story == null ? null : asString(story.get("userEmail"));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Long resolveCommentPostId(Long commentId) {
        if (commentId == null) return null;
        try {
            Map<String, Object> comment = restTemplate.getForObject(LikeConstants.COMMENT_SERVICE_URL, Map.class, commentId);
            if (comment == null) return null;
            Object postId = comment.get("postId");
            if (postId instanceof Number) return ((Number) postId).longValue();
            if (postId != null && String.valueOf(postId).trim().matches("\\d+")) return Long.parseLong(String.valueOf(postId).trim());
        } catch (Exception ignored) {}
        return null;
    }

    private String asString(Object value) {
        if (value == null) return null;
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }
}
