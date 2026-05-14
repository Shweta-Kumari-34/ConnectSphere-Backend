package com.connectsphere.notification.dto;

import com.connectsphere.notification.entity.Notification;
import java.util.List;

public class NotificationPageResponse {
    private List<Notification> notifications;
    private int page;
    private int size;
    private boolean hasMore;
    private long unreadCount;

    public List<Notification> getNotifications() { return notifications; }
    public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}
