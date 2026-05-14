package com.connectsphere.notification.service;
import com.connectsphere.notification.dto.NotificationPageResponse;
import com.connectsphere.notification.entity.Notification;
import java.util.List;

/**
 * <h1>NotificationService Interface</h1>
 * <p>The communication backbone of ConnectSphere, responsible for delivering timely alerts 
 * and maintaining user engagement through in-app and external notifications.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *     <li><b>Alert Lifecycle:</b> Creation, persistence, and state management (Read/Unread) of notifications.</li>
 *     <li><b>Bulk Processing:</b> Capability to send system-wide alerts to multiple recipients.</li>
 *     <li><b>Preference Management:</b> Handling user-specific notification settings and mute windows.</li>
 *     <li><b>Multi-Channel Delivery:</b> Support for in-app feeds, real-time SSE alerts, and email notifications.</li>
 * </ul>
 * 
 * <h2>Notification Pipeline:</h2>
 * <pre>
 * graph LR
 *     Source[Microservice Event] --> Factory{Create Alert}
 *     Factory --> Prefs{Check Settings}
 *     Prefs -- Allowed --> DB[(Persistence)]
 *     Prefs -- Muted --> Log[Log Only]
 *     DB --> Feed[In-App Feed]
 *     DB --> RealTime[SSE/Push Channel]
 * </pre>
 */
public interface NotificationService {

    /**
     * Creates a basic in-app notification for a user.
     */
    Notification createNotification(String recipientEmail, String senderEmail, String type, String message, Long referenceId);

    /**
     * Creates a rich notification with deep-linking and content metadata.
     */
    Notification createNotification(String recipientEmail, String senderEmail, String type, String message, Long referenceId, String actionUrl, String referenceType);

    /**
     * Dispatches system alerts to a list of recipients (Admin/System use).
     */
    void sendBulkNotification(List<String> recipientEmails, String type, String message);

    /**
     * Fetches all notification history for a user (unfiltered).
     */
    List<Notification> getNotifications(String recipientEmail);

    /**
     * Retrieves a paginated slice of notifications for optimized frontend loading.
     */
    NotificationPageResponse getNotifications(String recipientEmail, int page, int size);

    /**
     * Counts all unread alerts for a specific user.
     */
    long getUnreadCount(String recipientEmail);

    /**
     * Updates notification state to 'Read'.
     */
    void markAsRead(Long id);

    /**
     * Updates notification state to 'Unread'.
     */
    void markAsUnread(Long id);

    /**
     * Marks all notifications for a user as 'Read' in a single operation.
     */
    void markAllAsRead(String recipientEmail);

    /**
     * Permanently removes a notification.
     */
    void deleteNotification(Long id);

    /**
     * Triggers an external email alert for high-priority notifications.
     */
    void sendEmailAlert(String recipientEmail, String subject, String body);

    /**
     * Admin view: Lists all notifications in the system.
     */
    List<Notification> getAll();

    /**
     * Retrieves user-specific notification preferences.
     */
    com.connectsphere.notification.entity.NotificationSettings getSettings(String recipientEmail);

    /**
     * Updates notification delivery preferences.
     */
    com.connectsphere.notification.entity.NotificationSettings updateSettings(String recipientEmail, com.connectsphere.notification.entity.NotificationSettings settings);

    /**
     * Temporarily silences all notifications for a user.
     */
    void mute(String recipientEmail, java.time.LocalDateTime until);

    /**
     * Re-enables notification delivery for a muted user.
     */
    void unmute(String recipientEmail);
}
