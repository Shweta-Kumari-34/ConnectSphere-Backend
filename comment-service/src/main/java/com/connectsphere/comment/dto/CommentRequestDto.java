package com.connectsphere.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for creating a new comment or a reply.
 * <p>
 * This payload is submitted by the frontend when a user comments on a post/reel
 * or replies to an existing comment.
 * </p>
 *
 * <h3>Payload Context</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class CommentRequestDto {
 *         +Long postId
 *         +Long parentId
 *         +String content
 *     }
 *     class CommentController {
 *         +createComment(CommentRequestDto)
 *     }
 *     CommentRequestDto --> CommentController : Submit
 * </pre>
 */
public class CommentRequestDto {
    @NotNull(message = "Post ID is required")
    private Long postId;
    // Null means top-level comment; non-null means reply to parent comment.
    private Long parentId; // null for top-level, set for reply
    @NotBlank(message = "Content is required")
    private String content;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
