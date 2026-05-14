package com.connectsphere.media.controller;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.service.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * <h1>MediaController</h1>
 * <p>REST API endpoints for handling standard media (images, videos) associated with Posts, 
 * as well as the lifecycle of 24-hour ephemeral Stories.</p>
 * 
 * <h2>API Flow Overview:</h2>
 * <pre>
 * graph TD
 *     A[Frontend Client] -->|HTTP POST /media/upload/file| B(API Gateway)
 *     B -->|Injects X-User-Email| C[MediaController]
 *     C -->|Passes MultipartFile| D{MediaService}
 *     D -->|File I/O| E[(Disk Storage)]
 *     D -->|Persists Metadata| F[(MySQL DB)]
 * </pre>
 * 
 * <h2>Key Capabilities:</h2>
 * <ul>
 *     <li><b>File Uploads:</b> Supports both physical file uploads via {@link org.springframework.web.multipart.MultipartFile} and backward-compatible URL strings.</li>
 *     <li><b>Story Management:</b> Endpoints for viewing, commenting, and tracking unique views on ephemeral stories.</li>
 *     <li><b>Gateway Authenticated:</b> Relies on headers injected securely from the centralized auth layer.</li>
 * </ul>
 */
@RestController
@RequestMapping("/media")
public class MediaController {
    // Handles media and story transport concerns; business logic lives in MediaService.
    private final MediaService mediaService;
    public MediaController(MediaService mediaService) { this.mediaService = mediaService; }

    // ============================================================
    // MEDIA UPLOAD
    // ============================================================

    /** Upload media by URL (backwards compatibility) */
    @PostMapping("/upload")
    public ResponseEntity<Media> upload(@RequestHeader("X-User-Email") String userEmail,
                                         @RequestParam Long postId, @RequestParam String mediaUrl,
                                         @RequestParam(required = false) String mediaType) {
        // Legacy URL-based upload flow retained for backward compatibility.
        return ResponseEntity.ok(mediaService.uploadMedia(userEmail, postId, mediaUrl, mediaType));
    }

    /** Upload actual file (JPEG, PNG, WebP, MP4) — validated with configurable size limits */
    @PostMapping("/upload/file")
    public ResponseEntity<Media> uploadFile(@RequestHeader("X-User-Email") String userEmail,
                                             @RequestParam Long postId,
                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.uploadFile(userEmail, postId, file));
    }

    // ============================================================
    // MEDIA RETRIEVAL & DELETION
    // ============================================================

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Media>> getByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(mediaService.getMediaByPost(postId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Media> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getMediaById(id)
                .orElseThrow(() -> new RuntimeException("Media not found")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok("Media soft-deleted");
    }

    /** Soft-delete all media for a post (called when a post is deleted) */
    @DeleteMapping("/post/{postId}")
    public ResponseEntity<String> deleteMediaByPost(@PathVariable Long postId) {
        mediaService.softDeleteByPost(postId);
        return ResponseEntity.ok("Media for post soft-deleted");
    }

    // ============================================================
    // STORIES
    // ============================================================

    /** Create story from URL */
    @PostMapping("/stories")
    public ResponseEntity<Story> createStory(@RequestHeader("X-User-Email") String userEmail,
                                              @RequestParam String mediaUrl,
                                              @RequestParam(required = false) String caption) {
        // URL-based story creation (file-based path is `/stories/upload`).
        return ResponseEntity.ok(mediaService.createStory(userEmail, mediaUrl, caption));
    }

    /** Create story from file upload (JPEG, PNG, WebP, MP4) */
    @PostMapping("/stories/upload")
    public ResponseEntity<Story> createStoryFromFile(@RequestHeader("X-User-Email") String userEmail,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String caption) {
        return ResponseEntity.ok(mediaService.createStoryFromFile(userEmail, file, caption));
    }

    @GetMapping("/stories/active")
    public ResponseEntity<List<Story>> getActiveStories() {
        return ResponseEntity.ok(mediaService.getActiveStories());
    }

    @GetMapping("/stories/user/{userEmail}")
    public ResponseEntity<List<Story>> getUserStories(@PathVariable String userEmail) {
        return ResponseEntity.ok(mediaService.getUserStories(userEmail));
    }

    @GetMapping("/stories/{id}")
    public ResponseEntity<Story> getStoryById(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getStoryById(id));
    }

    @PostMapping("/stories/{id}/view")
    public ResponseEntity<String> viewStory(@PathVariable Long id,
                                            @RequestHeader("X-User-Email") String viewerEmail) {
        // Records per-user view; duplicate handling is managed in service/repository.
        mediaService.viewStory(id, viewerEmail);
        return ResponseEntity.ok("Story view recorded");
    }

    @DeleteMapping("/stories/{id}")
    public ResponseEntity<String> deleteStory(@PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        mediaService.deleteStory(id, userEmail);
        return ResponseEntity.ok("Story deleted");
    }

    @PostMapping("/stories/{id}/comment")
    public ResponseEntity<String> commentOnStory(@PathVariable Long id,
                                                 @RequestHeader("X-User-Email") String userEmail,
                                                 @RequestParam String content) {
        mediaService.commentOnStory(id, userEmail, content);
        return ResponseEntity.ok("Comment sent");
    }

    @GetMapping("/test")
    public String test() { return "Media Service is running"; }
}
