package com.connectsphere.notification.repository;
import com.connectsphere.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Notification} entities.
 * <p>
 * Provides paginated fetching, unread counts, and targeted deduplication queries
 * for the notification feed.
 * </p>
 *
 * <h3>Repository Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class NotificationRepository {
 *         +findByRecipientEmailOrderByCreatedAtDesc()
 *         +countByRecipientEmailAndIsReadFalse()
 *     }
 *     NotificationService --> NotificationRepository : Fetches & Saves
 * </pre>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);
    long countByRecipientEmailAndIsReadFalse(String recipientEmail);
    List<Notification> findByRecipientEmailAndIsReadFalse(String recipientEmail);
    java.util.Optional<Notification> findTopByRecipientEmailAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(String recipientEmail, String type, Long referenceId);
}
