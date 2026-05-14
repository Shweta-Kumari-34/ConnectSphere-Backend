package com.connectsphere.post.dto;

/**
 * DTO for user-submitted reel comments.
 *
 * <h3>Payload Structure</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class ReelCommentRequestDto {
 *         +String content
 *     }
 * </pre>
 */
public class ReelCommentRequestDto {
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
