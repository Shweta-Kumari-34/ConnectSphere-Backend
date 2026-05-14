package com.connectsphere.like.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * <h1>LikeEntity</h1>
 * <p>Represents a user's reaction or engagement with a specific platform target (Post, Reel, Story, etc.).</p>
 * 
 * <h2>Data Modeling Flow:</h2>
 * <pre>
 * graph TD
 *     A[User Reaction] --> B{LikeEntity}
 *     B -->|Persists| C[(SQL Database)]
 *     C -->|Unique Constraint| D[targetId + targetType + userEmail]
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Polymorphic Relations:</b> Uses {@code targetId} and {@code targetType} instead of strict foreign keys.</li>
 *     <li><b>Integrity:</b> Enforces database-level unique constraints so a user can only have one active reaction per target.</li>
 *     <li><b>Emotional Nuance:</b> Stores the specific {@code reactionType} (e.g., LIKE, LOVE, HAHA).</li>
 * </ul>
 */
@Entity
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(columnNames = {"targetId", "targetType", "userEmail"}))
public class LikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long targetId; // postId or commentId

    @Column(nullable = false)
    private String targetType; // POST or COMMENT

    @Column(nullable = false)
    private String userEmail;

    private String reactionType; // LIKE, LOVE, HAHA, SAD, ANGRY

    private LocalDateTime createdAt;

    public LikeEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
