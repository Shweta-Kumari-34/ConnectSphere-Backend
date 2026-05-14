package com.connectsphere.post.service.impl;

import com.connectsphere.post.entity.Reel;
import com.connectsphere.post.entity.ReelComment;
import com.connectsphere.post.dto.NotificationEvent;
import com.connectsphere.post.producer.NotificationEventProducer;
import com.connectsphere.post.repository.ReelCommentRepository;
import com.connectsphere.post.repository.ReelRepository;
import com.connectsphere.post.service.ReelService;
import com.connectsphere.post.util.PostConstants;
import com.connectsphere.post.exception.ResourceNotFoundException;
import com.connectsphere.post.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <h1>ReelServiceImpl</h1>
 * <p>Implementation of {@link ReelService} that handles the storage, retrieval, 
 * and engagement processing of short-form video content.</p>
 * 
 * <h2>Reel Upload & Interaction Flow:</h2>
 * <pre>
 * sequenceDiagram
 *     Actor->>ReelService: uploadReel(VideoFile)
 *     ReelService->>Disk: Store Video & Generate URL
 *     ReelService->>DB: Save Reel Entity
 *     Actor->>ReelService: addComment()
 *     ReelService->>DB: Save ReelComment
 *     ReelService->>RabbitMQ: Dispatch NotificationEvent
 *     Note over RabbitMQ: Includes Video URL for UI Thumbnails
 * </pre>
 * 
 * <h2>Key Technical Features:</h2>
 * <ul>
 *     <li><b>Local Video Storage:</b> Safely manages video file I/O operations and automatic disk cleanup on deletion.</li>
 *     <li><b>Asynchronous Notifications:</b> Uses RabbitMQ to alert creators of new comments without blocking the API response.</li>
 *     <li><b>Feed Generation:</b> Excludes the user's own reels from discovery feeds to enhance engagement.</li>
 *     <li><b>Robust Security:</b> Validates ownership and admin roles before allowing destructive operations like deletion.</li>
 * </ul>
 */
@Service
public class ReelServiceImpl implements ReelService {

    private static final Logger log = LoggerFactory.getLogger(ReelServiceImpl.class);

    private final ReelRepository reelRepository;
    private final ReelCommentRepository reelCommentRepository;
    private final NotificationEventProducer notificationEventProducer;
    private final Path rootPath = Paths.get(PostConstants.UPLOADS_REELS_DIR);

    public ReelServiceImpl(ReelRepository reelRepository,
                           ReelCommentRepository reelCommentRepository,
                           NotificationEventProducer notificationEventProducer) {
        this.reelRepository = reelRepository;
        this.reelCommentRepository = reelCommentRepository;
        this.notificationEventProducer = notificationEventProducer;
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            log.error("Could not initialize storage: {}", e.getMessage());
        }
    }

    @Override
    public Reel uploadReel(String userEmail, MultipartFile file, String caption, String visibility) {
        if (file == null || file.isEmpty()) {
            throw new ResourceNotFoundException("Cannot upload empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;

        try {
            Path destinationFile = this.rootPath.resolve(filename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            
            Reel reel = new Reel();
            reel.setUserEmail(userEmail != null ? userEmail : PostConstants.ANONYMOUS_EMAIL);
            reel.setVideoUrl(PostConstants.UPLOADS_REELS_URL_PREFIX + filename);
            reel.setCaption(caption);
            reel.setVisibility(visibility != null ? visibility : PostConstants.VISIBILITY_PUBLIC);
            
            return reelRepository.save(reel);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Failed to store reel: " + e.getMessage());
        }
    }

    @Override
    public List<Reel> getFeedForUser(String userEmail) {
        return reelRepository.findPublicReelsExcludingUser(userEmail);
    }

    @Override
    public List<Reel> getUserReels(String userEmail) {
        return reelRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Override
    public Reel getReelById(Long reelId) {
        return reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
    }

    @Override
    public void deleteReel(String userEmail, String userRole, Long reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new UnauthorizedException("Authentication required to delete reel");
        }

        boolean isAdmin = userRole != null && "ADMIN".equalsIgnoreCase(userRole.trim());
        boolean isOwner = userEmail.equalsIgnoreCase(reel.getUserEmail());

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Only the user who posted this reel can delete it");
        }

        String videoUrl = reel.getVideoUrl();
        if (videoUrl != null && videoUrl.startsWith(PostConstants.UPLOADS_REELS_URL_PREFIX)) {
            String filename = videoUrl.substring(PostConstants.UPLOADS_REELS_URL_PREFIX.length());
            Path filePath = rootPath.resolve(filename).normalize().toAbsolutePath();
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {}
        }

        reelCommentRepository.deleteByReelId(reelId);
        reelRepository.delete(reel);
    }

    @Override
    public ReelComment addComment(String userEmail, Long reelId, String content) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

        if (content == null || content.trim().isEmpty()) {
            throw new ResourceNotFoundException("Comment cannot be empty");
        }

        ReelComment comment = new ReelComment();
        comment.setReelId(reel.getId());
        comment.setUserEmail(userEmail != null ? userEmail : PostConstants.ANONYMOUS_EMAIL);
        comment.setContent(content.trim());
        ReelComment saved = reelCommentRepository.save(comment);
        sendReelCommentNotification(reel, saved);
        return saved;
    }

    @Override
    public List<ReelComment> getComments(Long reelId) {
        return reelCommentRepository.findByReelIdOrderByCreatedAtAsc(reelId);
    }

    private void sendReelCommentNotification(Reel reel, ReelComment comment) {
        String actorEmail = comment.getUserEmail();
        String recipientEmail = reel.getUserEmail();
        if (recipientEmail == null || actorEmail == null || recipientEmail.equalsIgnoreCase(actorEmail)) {
            return;
        }
        try {
            NotificationEvent event = new NotificationEvent();
            event.setRecipientEmail(recipientEmail);
            event.setActorEmail(actorEmail);
            event.setType(PostConstants.NOTIFICATION_TYPE_REEL_COMMENT);
            event.setTargetId(reel.getId());
            event.setDeepLinkUrl(PostConstants.DEEP_LINK_REEL + reel.getId());
            event.setMessage(actorEmail.split("@")[0] + " commented on your reel");
            event.setPriority(PostConstants.NOTIFICATION_PRIORITY_NORMAL);
            // Best-effort context for the notification UI (safe if null).
            String caption = reel.getCaption();
            String mediaUrl = reel.getVideoUrl();
            if (caption != null || mediaUrl != null) {
                String safeCaption = caption == null ? "null" : "\"" + caption.replace("\"", "\\\"") + "\"";
                String safeMediaUrl = mediaUrl == null ? "null" : "\"" + mediaUrl.replace("\"", "\\\"") + "\"";
                event.setMetadata(String.format("{\"caption\":%s,\"mediaUrl\":%s}", safeCaption, safeMediaUrl));
            }
            event.setCreatedAt(LocalDateTime.now());
            notificationEventProducer.publish(event);
        } catch (Exception ignored) {
            // Notification failures must not block comment creation.
        }
    }
}
