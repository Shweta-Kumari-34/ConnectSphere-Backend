package com.connectsphere.follow.util;

// Shared constant values used across follow service.
public final class FollowConstants {
    // Utility class; prevent instantiation.
    private FollowConstants() {}

    // Notification type sent when a follow action succeeds.
    public static final String NOTIFICATION_TYPE_FOLLOW = "FOLLOW";
    // High-priority notification for user engagement actions.
    public static final String NOTIFICATION_PRIORITY_HIGH = "HIGH";
    
    // Default active status for follow relation.
    public static final String STATUS_ACTIVE = "ACTIVE";
    
    // Base deep link path for profile navigation in frontend.
    public static final String DEEP_LINK_USER = "/user/";
}
