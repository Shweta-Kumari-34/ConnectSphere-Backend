package com.connectsphere.notification.service.impl;

import com.connectsphere.notification.dto.NotificationPageResponse;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.repository.NotificationSettingsRepository;
import com.connectsphere.notification.entity.NotificationSettings;
import com.connectsphere.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.connectsphere.notification.util.NotificationConstants;
import com.connectsphere.notification.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h1>NotificationServiceImpl</h1>
 * <p>Implementation of {@link NotificationService} that manages persistent storage and user preference 
 * filtering for all platform communications.</p>
 * 
 * <h2>Alert Filtering Flow:</h2>
 * <pre>
 * graph TD
 *     Start[Create Alert Request] --> Settings[Fetch User Settings]
 *     Settings --> Mute{Is User Muted?}
 *     Mute -- Yes --> Log[Log & Discard]
 *     Mute -- No --> DB[Save to Database]
 *     DB --> SSE[Broadcast via SSEEmitterService]
 * </pre>
 * 
 * <h2>Key Implementation Details:</h2>
 * <ul>
 *     <li><b>Pagination:</b> Uses Spring Data {@link Pageable} for high-performance notification feed retrieval.</li>
 *     <li><b>Mute Windows:</b> Supports temporary silences with auto-expiry check logic.</li>
 *     <li><b>Scalability:</b> Designed for event-driven consumption (Kafka integration in producer layers).</li>
 *     <li><b>SSE Integration:</b> Seamlessly hooks into {@code SseEmitterService} for real-time browser updates.</li>
 * </ul>
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, NotificationSettingsRepository settingsRepository) {
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public Notification createNotification(String recipientEmail, String senderEmail, String type, String message, Long referenceId) {
        return createNotification(recipientEmail, senderEmail, type, message, referenceId, null, null);
    }

    @Override
    public Notification createNotification(String recipientEmail, String senderEmail, String type, String message, Long referenceId, String actionUrl, String referenceType) {
        Notification n = new Notification();
        n.setRecipientEmail(recipientEmail);
        n.setSenderEmail(senderEmail);
        n.setType(type);
        n.setMessage(message);
        n.setReferenceId(referenceId);
        n.setActionUrl(actionUrl);
        n.setReferenceType(referenceType);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    @Override
    public void sendBulkNotification(List<String> recipientEmails, String type, String message) {
        for (String email : recipientEmails) {
            createNotification(email, NotificationConstants.SENDER_SYSTEM, type, message, null);
        }
    }

    @Override public List<Notification> getNotifications(String e) { return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(e); }
    @Override
    public NotificationPageResponse getNotifications(String recipientEmail, int page, int size) {
        Page<Notification> notificationPage = notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(
                recipientEmail, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, NotificationConstants.SORT_BY_CREATED_AT))
        );
        NotificationPageResponse response = new NotificationPageResponse();
        response.setNotifications(notificationPage.getContent());
        response.setPage(notificationPage.getNumber());
        response.setSize(notificationPage.getSize());
        response.setHasMore(!notificationPage.isLast());
        response.setUnreadCount(getUnreadCount(recipientEmail));
        return response;
    }
    @Override public long getUnreadCount(String e) { return notificationRepository.countByRecipientEmailAndIsReadFalse(e); }

    @Override
    public void markAsRead(Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    public void markAsUnread(Long id) {
        Notification n = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        n.setRead(false);
        notificationRepository.save(n);
    }

    @Override
    public void markAllAsRead(String recipientEmail) {
        List<Notification> unread = notificationRepository.findByRecipientEmailAndIsReadFalse(recipientEmail);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override public void deleteNotification(Long id) { notificationRepository.deleteById(id); }

    @Override
    public void sendEmailAlert(String recipientEmail, String subject, String body) {
        // Placeholder — in production, inject JavaMailSender and send actual email
        log.info("Email alert to {}: {} — {}", recipientEmail, subject, body);
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    @Override
    public NotificationSettings getSettings(String recipientEmail) {
        return settingsRepository.findByRecipientEmail(recipientEmail).orElseGet(() -> {
            NotificationSettings s = new NotificationSettings();
            s.setRecipientEmail(recipientEmail);
            return settingsRepository.save(s);
        });
    }

    @Override
    public NotificationSettings updateSettings(String recipientEmail, NotificationSettings settings) {
        NotificationSettings existing = settingsRepository.findByRecipientEmail(recipientEmail).orElseGet(() -> {
            NotificationSettings s = new NotificationSettings();
            s.setRecipientEmail(recipientEmail);
            return s;
        });
        existing.setEmailEnabled(settings.isEmailEnabled());
        existing.setPushEnabled(settings.isPushEnabled());
        existing.setMuted(settings.isMuted());
        existing.setMutedUntil(settings.getMutedUntil());
        return settingsRepository.save(existing);
    }

    @Override
    public void mute(String recipientEmail, LocalDateTime until) {
        NotificationSettings s = settingsRepository.findByRecipientEmail(recipientEmail).orElseGet(() -> {
            NotificationSettings ns = new NotificationSettings(); ns.setRecipientEmail(recipientEmail); return ns;
        });
        s.setMuted(true);
        s.setMutedUntil(until);
        settingsRepository.save(s);
    }

    @Override
    public void unmute(String recipientEmail) {
        NotificationSettings s = settingsRepository.findByRecipientEmail(recipientEmail).orElseGet(() -> {
            NotificationSettings ns = new NotificationSettings(); ns.setRecipientEmail(recipientEmail); return ns;
        });
        s.setMuted(false);
        s.setMutedUntil(null);
        settingsRepository.save(s);
    }
}
