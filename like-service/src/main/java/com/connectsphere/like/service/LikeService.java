package com.connectsphere.like.service;

import com.connectsphere.like.entity.LikeEntity;
import java.util.List;
import java.util.Map;

/**
 * <h1>LikeService Interface</h1>
 * <p>Centralized contract for managing engagement and emotional reactions across the ConnectSphere ecosystem.</p>
 * 
 * <h2>Core Functions:</h2>
 * <ul>
 *     <li><b>Polymorphic Engagement:</b> Unified interface for liking Posts, Reels, Comments, and Stories.</li>
 *     <li><b>Reaction Variety:</b> Supports multiple reaction types (Like, Love, Haha, etc.) beyond binary likes.</li>
 *     <li><b>Engagement Metrics:</b> Aggregates counts and summaries for content creators and analytics.</li>
 *     <li><b>Status Tracking:</b> Real-time verification of user interaction status for UI feedback.</li>
 * </ul>
 * 
 * <h2>Engagement Lifecycle:</h2>
 * <pre>
 * graph TD
 *     A[User] -->|Interact| B{Has Liked?}
 *     B -- No --> C[Create Reaction]
 *     B -- Yes --> D[Update/Change Reaction]
 *     B -- Yes --> E[Unlike/Remove]
 *     C --> F[Notify Creator]
 *     D --> F
 *     F --> G[(Engage DB)]
 * </pre>
 */
public interface LikeService {

    /**
     * Records a new reaction for a specific content target.
     */
    LikeEntity likeTarget(String userEmail, Long targetId, String targetType, String reactionType);

    /**
     * Removes an existing reaction.
     */
    void unlikeTarget(String userEmail, Long targetId, String targetType);

    /**
     * Verifies if a user has already interacted with the target content.
     */
    boolean hasLiked(String userEmail, Long targetId, String targetType);

    /**
     * Retrieves a list of all users who reacted to a specific target.
     */
    List<LikeEntity> getLikesByTarget(Long targetId, String targetType);

    /**
     * Fetches all engagement history for a specific user.
     */
    List<LikeEntity> getLikesByUser(String userEmail);

    /**
     * Returns the total engagement count for a piece of content.
     */
    long getLikeCount(Long targetId, String targetType);

    /**
     * Filters engagement count by a specific reaction type (e.g., only "Love" reactions).
     */
    long getLikeCountByType(Long targetId, String targetType, String reactionType);

    /**
     * Generates a breakdown of all reaction types for a content item (Reaction Map).
     */
    Map<String, Long> getReactionSummary(Long targetId, String targetType);

    /**
     * Swaps an existing reaction with a new type (Ownership verified).
     */
    LikeEntity changeReaction(String userEmail, Long targetId, String targetType, String newReaction);
}
