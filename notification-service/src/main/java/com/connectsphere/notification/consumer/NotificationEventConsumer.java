package com.connectsphere.notification.consumer;

import com.connectsphere.notification.dto.NotificationEvent;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.repository.NotificationSettingsRepository;
import com.connectsphere.notification.producer.NotificationProducer;
import com.connectsphere.notification.sse.SseEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * NotificationEventConsumer — Listens to RabbitMQ for incoming events and turns them into notifications.
 *
 * Workflow:
 *   1. Another service (e.g., Like Service, Follow Service, Post Service) publishes an event to the RabbitMQ queue.
 *   2. This consumer automatically picks it up and processes it.
 *   3. The event is saved to the MySQL notifications table.
 *   4. The notification is pushed to the user in real time using two channels:
 *         a. WebSocket (STOMP) — if the user has a browser tab open
 *         b. SSE (Server-Sent Events) — as a fallback for non-WebSocket environments
 *   5. If the notification is HIGH priority and the user allows emails, an email is queued.
 */
@Component
public class NotificationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final SimpMessagingTemplate messagingTemplate;   // used for sending WebSocket messages
    private final NotificationProducer notificationProducer; // used to publish HIGH priority emails back to RabbitMQ
    private final SseEmitterService sseEmitterService;       // used to send real-time SSE events

    @Value("${app.notification.queue}") private String notificationQueue;

    public NotificationEventConsumer(
            NotificationRepository notificationRepository,
            NotificationSettingsRepository settingsRepository,
            SimpMessagingTemplate messagingTemplate,
            NotificationProducer notificationProducer,
            SseEmitterService sseEmitterService
    ) {
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationProducer = notificationProducer;
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * This method is automatically called whenever a message arrives in the RabbitMQ notification queue.
     * Spring AMQP handles the deserialization from JSON into a NotificationEvent object for us.
     *
     * What happens here step by step:
     *   Step 1 — Convert the raw event into a Notification database entity.
     *   Step 2 — Smart aggregation: if this is a LIKE and someone already liked the same post recently,
     *            we don't create a new notification. We update the existing one to say "Alice and 3 others liked your post".
     *   Step 3 — Save to MySQL so the user can see it when they open their notification panel.
     *   Step 4 — Count how many unread notifications the user has.
     *   Step 5 — Push the notification to the user's browser via WebSocket in real time.
     *   Step 6 — Check the user's notification settings (have they muted? is push enabled? is email enabled?).
     *   Step 7 — If push is allowed, also send via SSE as a backup channel.
     *   Step 8 — If it's HIGH priority (e.g., admin message) and email is allowed, trigger an email.
     */
    @RabbitListener(queues = "${app.notification.queue}")
    public void consume(NotificationEvent event) {

        // Step 1: Convert the incoming event payload into a Notification entity ready for saving.
        Notification notification = mapToEntity(event);

        // Step 2: Smart aggregation for LIKE events.
        // Instead of spamming "Bob liked your post", "Carol liked your post", "Dave liked your post",
        // we merge them into one: "Bob and 2 others liked your post".
        Notification saved;
        if ("LIKE".equalsIgnoreCase(notification.getType()) && notification.getReferenceId() != null && notification.getRecipientEmail() != null) {
            var opt = notificationRepository.findTopByRecipientEmailAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
                    notification.getRecipientEmail(), notification.getType(), notification.getReferenceId());
            if (opt.isPresent()) {
                Notification existing = opt.get();

                // Only aggregate if the previous LIKE notification is recent (within 1 hour).
                // Older ones get a fresh notification entry instead.
                if (existing.getCreatedAt() != null && existing.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1))) {
                    // Add the new actor to the comma-separated list stored in metadata.
                    String meta = existing.getMetadata();
                    String actor = notification.getSenderEmail() != null ? notification.getSenderEmail() : String.valueOf(notification.getActorId());
                    java.util.Set<String> actors = new java.util.LinkedHashSet<>();
                    if (meta != null && !meta.isBlank()) {
                        for (String a : meta.split(",")) if (!a.isBlank()) actors.add(a.trim());
                    }
                    actors.add(actor);
                    String newMeta = String.join(",", actors);
                    existing.setMetadata(newMeta);

                    // Rebuild the human-readable message: "Alice and 2 others liked your post"
                    String first = actors.iterator().next();
                    int others = actors.size() - 1;
                    String message = others > 0 ? String.format("%s and %d others liked your post", first, others) : String.format("%s liked your post", first);
                    existing.setMessage(message);
                    existing.setCreatedAt(LocalDateTime.now()); // bump the timestamp so it appears at the top
                    saved = notificationRepository.save(existing);
                    log.info("Aggregated LIKE notification id={} actors={}", saved.getId(), actors.size());
                } else {
                    // Old notification exists but it's stale — create a new one.
                    saved = notificationRepository.save(notification);
                }
            } else {
                // No existing LIKE notification for this post — create a fresh one.
                saved = notificationRepository.save(notification);
            }
        } else {
            // Step 3 (non-LIKE): Just save it directly — no aggregation needed.
            saved = notificationRepository.save(notification);
        }
        log.info("Notification created via RabbitMQ id={} type={} recipient={}", saved.getId(), saved.getType(), saved.getRecipientEmail());

        // Step 4: Determine the WebSocket topic key for this user (either their userId or email).
        String recipientTopicKey = resolveRecipientTopicKey(saved);

        // Step 4 continued: Count their total unread notifications — the frontend badge shows this number.
        long unreadCount = notificationRepository.countByRecipientEmailAndIsReadFalse(saved.getRecipientEmail());

        // Step 5: Push to the user's browser via WebSocket (STOMP protocol).
        // The frontend subscribes to these topics to receive real-time updates.
        messagingTemplate.convertAndSend("/topic/notifications/" + recipientTopicKey, saved);
        messagingTemplate.convertAndSend("/topic/notifications/count/" + recipientTopicKey, unreadCount);
        messagingTemplate.convertAndSendToUser(recipientTopicKey, "/queue/notifications", saved);
        messagingTemplate.convertAndSendToUser(recipientTopicKey, "/queue/notifications/count", unreadCount);

        // Step 6: Check if the user has turned off notifications or set a temporary mute.
        boolean pushAllowed = true;
        boolean emailAllowed = true;
        if (saved.getRecipientEmail() != null) {
            var opt = settingsRepository.findByRecipientEmail(saved.getRecipientEmail());
            if (opt.isPresent()) {
                var s = opt.get();
                pushAllowed = s.isPushEnabled() && !s.isMuted();
                if (s.isMuted() && s.getMutedUntil() != null) {
                    if (s.getMutedUntil().isAfter(java.time.LocalDateTime.now())) {
                        // Mute is still active — block push notifications.
                        pushAllowed = false;
                    } else {
                        // Mute has expired — automatically clear it so future notifications go through.
                        s.setMuted(false);
                        s.setMutedUntil(null);
                        settingsRepository.save(s);
                    }
                }
                emailAllowed = s.isEmailEnabled();
            }
        }

        // Step 7: Also send via SSE (Server-Sent Events) as a real-time fallback for
        // users whose browser may not maintain a persistent WebSocket connection.
        if (pushAllowed) {
            sseEmitterService.sendEvent(recipientTopicKey, saved);
            sseEmitterService.sendEvent(recipientTopicKey + ":count", unreadCount);
        }

        // Step 8: If the event is HIGH or CRITICAL priority and the user allows emails,
        // publish another message to RabbitMQ for the email service to pick up and send.
        if (("HIGH".equalsIgnoreCase(event.getPriority()) || "CRITICAL".equalsIgnoreCase(event.getPriority())) && emailAllowed) {
            notificationProducer.publishHighPriorityEmail(event);
        }
    }

    /**
     * Converts the RabbitMQ event DTO into a Notification entity that can be stored in MySQL.
     * Applies safe defaults for any missing fields (e.g., unknown actor becomes "SYSTEM").
     */
    private Notification mapToEntity(NotificationEvent event) {
        Notification n = new Notification();
        n.setRecipientId(event.getRecipientId());
        n.setActorId(event.getActorId());
        // If recipientEmail is missing, fall back to the numeric ID as a string identifier.
        n.setRecipientEmail(event.getRecipientEmail() == null ? String.valueOf(event.getRecipientId()) : event.getRecipientEmail());
        n.setSenderEmail(event.getActorEmail() == null ? "SYSTEM" : event.getActorEmail());
        n.setType(event.getType() == null ? "SYSTEM" : event.getType());
        n.setTargetId(event.getTargetId());
        n.setReferenceId(event.getTargetId()); // same as targetId — used for aggregation lookups
        n.setDeepLinkUrl(event.getDeepLinkUrl());
        n.setActionUrl(event.getDeepLinkUrl());
        n.setMessage(event.getMessage());
        n.setPriority(event.getPriority() == null ? "NORMAL" : event.getPriority());
        n.setMetadata(event.getMetadata());
        n.setRead(false); // all new notifications start as unread
        n.setCreatedAt(event.getCreatedAt() == null ? LocalDateTime.now() : event.getCreatedAt());

        // Extract referenceType from metadata JSON (e.g., POST, REEL, STORY) so the
        // frontend can show the correct deep-link and thumbnail for each content type
        if (event.getMetadata() != null && !event.getMetadata().isBlank()) {
            String meta = event.getMetadata();
            String refType = extractJsonString(meta, "referenceType");
            if (refType != null) {
                n.setReferenceType(refType);
            }
        }

        return n;
    }

    /**
     * Minimal inline JSON string extractor — avoids a Jackson dependency in this consumer.
     * Extracts the value of a top-level string key from a raw JSON string.
     */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        int end = json.indexOf('"', start);
        if (end < start) return null;
        String value = json.substring(start, end).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Determines which WebSocket topic to push the notification to.
     * Prefers the numeric userId (more efficient for routing), falls back to email.
     */
    private String resolveRecipientTopicKey(Notification n) {
        if (n.getRecipientId() != null) {
            return String.valueOf(n.getRecipientId());
        }
        return n.getRecipientEmail();
    }
}
