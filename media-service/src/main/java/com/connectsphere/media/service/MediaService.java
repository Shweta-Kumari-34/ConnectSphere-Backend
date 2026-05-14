package com.connectsphere.media.service;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

/**
 * <h1>MediaService Interface</h1>
 * <p>Handles the lifecycle of visual assets and ephemeral content within the ConnectSphere ecosystem.</p>
 * 
 * <h2>Core Functions:</h2>
 * <ul>
 *     <li><b>Asset Management:</b> Upload and storage of high-quality images and videos for posts.</li>
 *     <li><b>Ephemeral Stories:</b> Creation and retrieval of 24-hour temporary content.</li>
 *     <li><b>File Integrity:</b> Validation of MIME types and file sizes to ensure platform performance.</li>
 *     <li><b>Story Engagement:</b> Support for story views and direct replies/comments.</li>
 * </ul>
 * 
 * <h2>Media Lifecycle:</h2>
 * <pre>
 * graph TD
 *     A[User Upload] -->|Validation| B{Is File Valid?}
 *     B -- No --> C[Error: BadRequest]
 *     B -- Yes --> D[Store in Local CDN]
 *     D --> E[Save Record in DB]
 *     E -- Story --> F[Auto-Expire (24h)]
 *     E -- Post Media --> G[Permanent Storage]
 * </pre>
 */
public interface MediaService {

    /**
     * Uploads media via a direct URL (Backwards compatibility).
     */
    Media uploadMedia(String userEmail, Long postId, String mediaUrl, String mediaType);

    /**
     * Uploads a physical file to the local storage and records it in the database.
     */
    Media uploadFile(String userEmail, Long postId, MultipartFile file);

    /**
     * Retrieves all active media attached to a specific post.
     */
    List<Media> getMediaByPost(Long postId);

    /**
     * Fetches a single media record by ID.
     */
    Optional<Media> getMediaById(Long id);

    /**
     * Marks a media record as deleted (Soft delete).
     */
    void deleteMedia(Long id);

    /**
     * Soft-deletes all media associated with a post (Cascade helper).
     */
    void softDeleteByPost(Long postId);

    /**
     * Creates a new ephemeral story using a media URL.
     */
    Story createStory(String userEmail, String mediaUrl, String caption);

    /**
     * Creates a new ephemeral story by uploading a physical file.
     */
    Story createStoryFromFile(String userEmail, MultipartFile file, String caption);

    /**
     * Retrieves all globally active stories (not expired).
     */
    List<Story> getActiveStories();

    /**
     * Retrieves all active stories for a specific user.
     */
    List<Story> getUserStories(String userEmail);

    /**
     * Fetches a story by ID.
     */
    Story getStoryById(Long id);

    /**
     * Records a view for a story and increments metrics.
     */
    void viewStory(Long storyId, String viewerEmail);

    /**
     * Deletes a story (Ownership verified).
     */
    void deleteStory(Long id, String userEmail);

    /**
     * Background task to remove stories that have passed their 24-hour window.
     */
    void expireOldStories();

    /**
     * Triggers a notification to the story owner for a new reply.
     */
    void commentOnStory(Long storyId, String userEmail, String content);
}
