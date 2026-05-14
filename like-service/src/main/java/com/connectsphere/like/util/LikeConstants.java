package com.connectsphere.like.util;

/**
 * <h1>LikeConstants</h1>
 * <p>A centralized configuration class defining static variables, target types, and routing 
 * URLs used throughout the Like Service ecosystem.</p>
 * 
 * <h2>Configuration Mapping Flow:</h2>
 * <pre>
 * graph LR
 *     A[Like Request] --> B{LikeConstants}
 *     B -->|Target Resolution| C(POST/REEL/STORY)
 *     B -->|Notification Type| D(POST_LIKE/etc)
 *     B -->|Cross-Service Route| E(http://localhost:8082)
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Deep Link Generation:</b> Provides base URLs for notification deep-linking back to the frontend.</li>
 *     <li><b>Cross-Service Resolution:</b> Hardcodes internal REST endpoint URLs for owner resolution.</li>
 *     <li><b>Consistency:</b> Prevents typos and magic strings by enforcing strong typing for target types.</li>
 * </ul>
 */
public final class LikeConstants {
    private LikeConstants() {}

    public static final String NOTIFICATION_TYPE_POST_LIKE = "POST_LIKE";
    public static final String NOTIFICATION_TYPE_REEL_LIKE = "REEL_LIKE";
    public static final String NOTIFICATION_TYPE_STORY_LIKE = "STORY_LIKE";
    public static final String NOTIFICATION_TYPE_COMMENT_LIKE = "COMMENT_LIKE";
    public static final String NOTIFICATION_PRIORITY_NORMAL = "NORMAL";
    
    public static final String TARGET_TYPE_POST = "POST";
    public static final String TARGET_TYPE_REEL = "REEL";
    public static final String TARGET_TYPE_STORY = "STORY";
    public static final String TARGET_TYPE_COMMENT = "COMMENT";
    
    public static final String REACTION_LIKE = "LIKE";
    
    public static final String DEEP_LINK_POST = "/posts?postId=";
    public static final String DEEP_LINK_REEL = "/reels?reelId=";
    public static final String DEEP_LINK_STORY = "/stories?storyId=";
    
    public static final String POST_SERVICE_URL = "http://localhost:8082/posts/{id}";
    public static final String REEL_SERVICE_URL = "http://localhost:8082/reels/{id}";
    public static final String STORY_SERVICE_URL = "http://localhost:8092/media/stories/{id}";
    public static final String COMMENT_SERVICE_URL = "http://localhost:8093/comments/{id}";
}
