package com.connectsphere.comment.util;

/**
 * Centralized constant definitions for the Comment Service.
 * <p>
 * Prevents magic strings across the service by organizing notification types,
 * priority levels, reference types, and external service URLs in one location.
 * </p>
 *
 * <h3>Constants Overview</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class CommentConstants {
 *         +String NOTIFICATION_TYPE_COMMENT
 *         +String NOTIFICATION_TYPE_REPLY
 *         +String DEEP_LINK_POST
 *         +String DEEP_LINK_REEL
 *     }
 * </pre>
 */
public final class CommentConstants {
    private CommentConstants() {}

    public static final String NOTIFICATION_TYPE_COMMENT = "COMMENT";
    public static final String NOTIFICATION_TYPE_REPLY = "REPLY";
    public static final String NOTIFICATION_PRIORITY_NORMAL = "NORMAL";
    
    public static final String REF_TYPE_POST = "POST";
    public static final String REF_TYPE_REEL = "REEL";
    
    public static final String DEEP_LINK_POST = "/posts?postId=";
    public static final String DEEP_LINK_REEL = "/reels?reelId=";
    
    public static final String POST_SERVICE_URL = "http://localhost:8082/posts/{id}";
    public static final String REEL_SERVICE_URL = "http://localhost:8082/reels/{id}";
}
