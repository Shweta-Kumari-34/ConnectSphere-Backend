package com.connectsphere.media.service.impl;

import com.connectsphere.media.config.UploadConfig;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import com.connectsphere.media.service.MediaService;
import com.connectsphere.media.dto.NotificationEvent;
import com.connectsphere.media.producer.NotificationEventProducer;
import com.connectsphere.media.util.MediaConstants;
import com.connectsphere.media.exception.ResourceNotFoundException;
import com.connectsphere.media.exception.UnauthorizedException;
import com.connectsphere.media.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>MediaServiceImpl</h1>
 * <p>Implementation of {@link MediaService} that manages file storage on a local CDN-simulated drive 
 * and handles ephemeral story lifecycles.</p>
 * 
 * <h2>Story Expiration Pipeline:</h2>
 * <pre>
 * sequenceDiagram
 *     User->>MediaService: createStory()
 *     MediaService->>Disk: Store File (UUID Filename)
 *     MediaService->>DB: Save Story (expiresAt = Now + 24h)
 *     loop Every 5 Minutes
 *         Scheduler->>MediaService: expireOldStories()
 *         MediaService->>DB: Delete from DB where expiresAt <= Now
 *     end
 * </pre>
 * 
 * <h2>Technical Features:</h2>
 * <ul>
 *     <li><b>File Validation:</b> Strict checks on MIME types and size limits via {@link UploadConfig}.</li>
 *     <li><b>Local CDN Storage:</b> Files are stored in a configurable local directory and served via a URL prefix.</li>
 *     <li><b>Engagement:</b> Increments view counts and triggers Kafka events for story replies.</li>
 *     <li><b>Caching:</b> High-performance caching for active stories to reduce recurring DB queries.</li>
 * </ul>
 */
@Service
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final MediaRepository mediaRepository;
    private final StoryRepository storyRepository;
    private final UploadConfig uploadConfig;
    private final NotificationEventProducer notificationEventProducer;
    private final RestTemplate restTemplate;

    public MediaServiceImpl(MediaRepository mediaRepository, StoryRepository storyRepository, 
                            UploadConfig uploadConfig, NotificationEventProducer notificationEventProducer,
                            RestTemplate restTemplate) {
        this.mediaRepository = mediaRepository;
        this.storyRepository = storyRepository;
        this.uploadConfig = uploadConfig;
        this.notificationEventProducer = notificationEventProducer;
        this.restTemplate = restTemplate;
    }

    // ============================================================
    // MEDIA UPLOAD (URL-based — for backwards compatibility)
    // ============================================================

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaByPost", key = "#a1"),
            @CacheEvict(value = "mediaByUserStories", key = "#a0"),
            @CacheEvict(value = "mediaActiveStories", allEntries = true)
    })
    public Media uploadMedia(String userEmail, Long postId, String mediaUrl, String mediaType) {
        Media media = new Media();
        media.setUserEmail(userEmail);
        media.setPostId(postId);
        media.setMediaUrl(mediaUrl);
        media.setMediaType(mediaType != null ? mediaType : MediaConstants.MEDIA_TYPE_IMAGE);
        media.setDeleted(false);
        return mediaRepository.save(media);
    }

    // ============================================================
    // MEDIA UPLOAD (File-based — actual file to local CDN store)
    // ============================================================

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaByPost", key = "#a1")
    })
    public Media uploadFile(String userEmail, Long postId, MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType();
        String mediaType = uploadConfig.isImage(contentType) ? MediaConstants.MEDIA_TYPE_IMAGE : MediaConstants.MEDIA_TYPE_VIDEO;
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + ext;

        // Save file to local CDN-simulated directory
        String savedUrl = saveFile(file, filename);

        Media media = new Media();
        media.setUserEmail(userEmail);
        media.setPostId(postId);
        media.setMediaUrl(savedUrl);
        media.setMediaType(mediaType);
        media.setMimeType(contentType);
        media.setSizeKb(file.getSize() / 1024);
        media.setDeleted(false);
        return mediaRepository.save(media);
    }

    // ============================================================
    // MEDIA RETRIEVAL & DELETION
    // ============================================================

    @Override
    @Cacheable(value = "mediaByPost", key = "#a0", unless = "#result == null")
    public List<Media> getMediaByPost(Long postId) {
        return mediaRepository.findByPostIdAndIsDeletedFalse(postId);
    }

    @Override
    @Cacheable(value = "mediaById", key = "#a0", unless = "#result == null")
    public Optional<Media> getMediaById(Long id) {
        return mediaRepository.findById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaById", key = "#a0"),
            @CacheEvict(value = "mediaByPost", allEntries = true)
    })
    public void deleteMedia(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));
        media.setDeleted(true); // Soft-delete for audit trail
        mediaRepository.save(media);
    }

    /** Soft-delete all media attached to a post (cascade on post delete) */
    @Override
    @CacheEvict(value = "mediaByPost", key = "#a0")
    public void softDeleteByPost(Long postId) {
        List<Media> mediaList = mediaRepository.findByPostIdAndIsDeletedFalse(postId);
        mediaList.forEach(m -> m.setDeleted(true));
        mediaRepository.saveAll(mediaList);
    }

    // ============================================================
    // STORIES
    // ============================================================

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaActiveStories", allEntries = true),
            @CacheEvict(value = "mediaByUserStories", key = "#a0")
    })
    public Story createStory(String userEmail, String mediaUrl, String caption) {
        validateStoryImageUrl(mediaUrl);

        Story story = new Story();
        story.setUserEmail(userEmail);
        story.setMediaUrl(mediaUrl);
        story.setCaption(caption);
        story.setMediaType(MediaConstants.MEDIA_TYPE_IMAGE);
        return storyRepository.save(story);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaActiveStories", allEntries = true),
            @CacheEvict(value = "mediaByUserStories", key = "#a0")
    })
    public Story createStoryFromFile(String userEmail, MultipartFile file, String caption) {
        validateFile(file);

        String contentType = file.getContentType();
        String ext = getExtension(file.getOriginalFilename());
        String filename = "story_" + UUID.randomUUID().toString() + ext;
        String mediaType = uploadConfig.isImage(contentType) ? MediaConstants.MEDIA_TYPE_IMAGE : MediaConstants.MEDIA_TYPE_VIDEO;

        String savedUrl = saveFile(file, filename);

        Story story = new Story();
        story.setUserEmail(userEmail);
        story.setMediaUrl(savedUrl);
        story.setCaption(caption);
        story.setMediaType(mediaType);
        return storyRepository.save(story);
    }

    @Override
    @Cacheable(value = "mediaActiveStories", key = "'active'", unless = "#result == null")
    public List<Story> getActiveStories() {
        expireOldStories();
        return storyRepository.findByActiveTrueAndExpiresAtAfter(LocalDateTime.now());
    }

    @Override
    @Cacheable(value = "mediaByUserStories", key = "#a0", unless = "#result == null")
    public List<Story> getUserStories(String userEmail) {
        expireOldStories();
        return storyRepository.findByUserEmailAndActiveTrueAndExpiresAtAfter(userEmail, LocalDateTime.now());
    }

    @Override
    @Cacheable(value = "mediaStoryById", key = "#a0", unless = "#result == null")
    public Story getStoryById(Long id) {
        return storyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaStoryById", key = "#a0"),
            @CacheEvict(value = "mediaActiveStories", allEntries = true),
            @CacheEvict(value = "mediaByUserStories", allEntries = true)
    })
    public void viewStory(Long storyId, String viewerEmail) {
        Story story = getActiveStoryOrThrow(storyId);
        if (!story.getUserEmail().equalsIgnoreCase(viewerEmail)) {
            story.setViewsCount(story.getViewsCount() + 1);
            storyRepository.save(story);
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "mediaStoryById", key = "#a0"),
            @CacheEvict(value = "mediaActiveStories", allEntries = true),
            @CacheEvict(value = "mediaByUserStories", key = "#a1")
    })
    public void deleteStory(Long id, String userEmail) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getUserEmail().equals(userEmail))
            throw new UnauthorizedException("Not authorized");
        storyRepository.delete(story);
    }

    /** Scheduled job: purge expired stories every 5 minutes */
    @Override
    @Scheduled(fixedRate = 300000)
    @Caching(evict = {
            @CacheEvict(value = "mediaActiveStories", allEntries = true),
            @CacheEvict(value = "mediaByUserStories", allEntries = true),
            @CacheEvict(value = "mediaStoryById", allEntries = true)
    })
    public void expireOldStories() {
        List<Story> expired = storyRepository.findByActiveTrueAndExpiresAtLessThanEqual(LocalDateTime.now());
        if (!expired.isEmpty()) {
            storyRepository.deleteAll(expired);
        }
    }

    // ============================================================
    // FILE VALIDATION & STORAGE HELPERS
    // ============================================================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (!uploadConfig.isAllowedType(contentType)) {
            throw new BadRequestException(
                "Unsupported file type: " + contentType +
                ". Allowed: JPEG, PNG, WebP, MP4");
        }
        long sizeKb = file.getSize() / 1024;
        long maxKb = uploadConfig.getMaxSizeKb(contentType);
        if (sizeKb > maxKb) {
            throw new BadRequestException(
                "File too large: " + sizeKb + "KB. Max allowed: " + maxKb + "KB");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (!uploadConfig.isImage(contentType) || !uploadConfig.getAllowedImageTypes().contains(contentType)) {
            throw new BadRequestException(
                "Unsupported story image type: " + contentType +
                ". Allowed: JPEG, PNG, WebP");
        }

        long sizeKb = file.getSize() / 1024;
        long maxKb = uploadConfig.getMaxImageSizeKb();
        if (sizeKb > maxKb) {
            throw new BadRequestException(
                "Image too large: " + sizeKb + "KB. Max allowed: " + maxKb + "KB");
        }
    }

    private String saveFile(MultipartFile file, String filename) {
        try {
            Path uploadPath = Paths.get(uploadConfig.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new IOException("Resolved upload path is invalid");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            // Return a proxy-safe relative URL; frontend serves it via /uploads route.
            return MediaConstants.UPLOADS_URL_PREFIX + filename;
        } catch (IOException e) {
            throw new ResourceNotFoundException("Failed to save file: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return MediaConstants.EXTENSION_BIN;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : MediaConstants.EXTENSION_BIN;
    }

    private Story getActiveStoryOrThrow(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.isActive() || story.getExpiresAt() == null || !story.getExpiresAt().isAfter(LocalDateTime.now())) {
            if (story.isActive()) {
                story.setActive(false);
                storyRepository.save(story);
            }
            throw new BadRequestException("Story has expired");
        }
        return story;
    }

    private void validateStoryImageUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            throw new BadRequestException("Story media URL is required");
        }

        String normalized = mediaUrl.toLowerCase();
        if (!(normalized.matches(".*\\.(jpg|jpeg|png|webp)($|\\?).*") || normalized.contains(MediaConstants.UPLOADS_URL_PREFIX))) {
            throw new BadRequestException("Stories currently support image URLs only");
        }
    }

    @Override
    public void commentOnStory(Long storyId, String userEmail, String content) {
        Story story = getStoryById(storyId);
        if (story == null) return;

        // In a real app, we might save this comment to a StoryComment table.
        // For now, we'll just trigger a notification to the story owner.
        
        String recipientEmail = story.getUserEmail();
        if (recipientEmail == null || recipientEmail.equalsIgnoreCase(userEmail)) {
            return;
        }

        try {
            NotificationEvent event = new NotificationEvent();
            event.setRecipientEmail(recipientEmail);
            event.setActorEmail(userEmail);
            event.setType(MediaConstants.NOTIFICATION_TYPE_STORY_COMMENT);
            event.setTargetId(storyId);
            event.setDeepLinkUrl(MediaConstants.DEEP_LINK_STORY + storyId);
            event.setMessage(userEmail.split("@")[0] + " replied to your story: " + content);
            event.setPriority(MediaConstants.NOTIFICATION_PRIORITY_NORMAL);
            event.setCreatedAt(LocalDateTime.now());

            // Add metadata for thumbnail preview in notification panel
            String mediaUrl = story.getMediaUrl();
            if (mediaUrl != null) {
                String safeImage = "\"" + mediaUrl.replace("\"", "\\\"") + "\"";
                event.setMetadata(String.format("{\"mediaUrl\":%s,\"referenceType\":\"STORY\"}", safeImage));
            }

            notificationEventProducer.publish(event);
        } catch (Exception ignored) {}
    }
}
