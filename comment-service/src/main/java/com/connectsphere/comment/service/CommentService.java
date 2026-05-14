package com.connectsphere.comment.service;

import com.connectsphere.comment.dto.CommentRequestDto;
import com.connectsphere.comment.entity.Comment;
import java.util.List;

/**
 * <h1>CommentService Interface</h1>
 * <p>Defines the contract for social interaction through comments and replies.</p>
 * 
 * <h2>Core Functions:</h2>
 * <ul>
 *     <li><b>CRUD:</b> Creation, retrieval, updating, and soft-deletion of comments.</li>
 *     <li><b>Engagement:</b> Like and unlike mechanisms for community feedback.</li>
 *     <li><b>Hierarchy:</b> Support for threaded conversations through parent-child relationships.</li>
 *     <li><b>Notifications:</b> Integration hooks for alerting post owners about new interactions.</li>
 * </ul>
 * 
 * <h2>Interaction Flow:</h2>
 * <pre>
 * graph TD
 *     A[User] -->|CommentRequest| B(CommentService)
 *     B -->|Persist| C[(Database)]
 *     B -->|Async| D{Notification System}
 *     D -->|Alert| E[Post Owner]
 *     C -->|Retrieve| F[Comment Feed]
 * </pre>
 */
public interface CommentService {

    /**
     * Adds a new comment or reply to a post/reel.
     */
    Comment addComment(String userEmail, CommentRequestDto request);

    /**
     * Retrieves all active comments for a specific post in chronological order.
     */
    List<Comment> getCommentsByPost(Long postId);

    /**
     * Retrieves all replies for a specific parent comment.
     */
    List<Comment> getReplies(Long parentId);

    /**
     * Fetches a single comment by its unique identifier.
     */
    Comment getCommentById(Long id);

    /**
     * Updates the content of an existing comment (Ownership verified).
     */
    Comment updateComment(Long id, String userEmail, String content);

    /**
     * Marks a comment as deleted (Ownership verified).
     */
    void deleteComment(Long id, String userEmail);

    /**
     * Fetches all comments made by a specific user.
     */
    List<Comment> getCommentsByUser(String userEmail);

    /**
     * Increments the like count for a comment.
     */
    void likeComment(Long id);

    /**
     * Decrements the like count for a comment.
     */
    void unlikeComment(Long id);

    /**
     * Returns the total number of active comments for a post.
     */
    long getCommentCount(Long postId);
}
