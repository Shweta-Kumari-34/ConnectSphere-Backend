package com.connectsphere.post.util;

/**
 * Centralized constants for the Post Service.
 * <p>
 * Eliminates magic strings for notification types, visibility modes, and static directory paths.
 * </p>
 *
 * <h3>Constants Overview</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class PostConstants {
 *         +String NOTIFICATION_TYPE_MENTION
 *         +String VISIBILITY_PUBLIC
 *         +String DEEP_LINK_POST
 *     }
 * </pre>
 */
public final class PostConstants {
    private PostConstants() {}

    public static final String NOTIFICATION_TYPE_MENTION = "MENTION";
    public static final String NOTIFICATION_TYPE_REEL_COMMENT = "REEL_COMMENT";
    public static final String NOTIFICATION_PRIORITY_NORMAL = "NORMAL";
    
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_PRIVATE = "PRIVATE";
    
    public static final String ROLE_ADMIN = "ADMIN";
    
    public static final String DEEP_LINK_POST = "/posts?postId=";
    public static final String DEEP_LINK_REEL = "/reels?reelId=";
    
    public static final String UPLOADS_REELS_DIR = "uploads/reels";
    public static final String UPLOADS_REELS_URL_PREFIX = "/uploads/reels/";
    
    public static final String ANONYMOUS_EMAIL = "anonymous@connectsphere.com";
}
