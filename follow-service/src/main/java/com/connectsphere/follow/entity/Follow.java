package com.connectsphere.follow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Follow edge between two users: follower -> following.
@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"followerEmail", "followingEmail"}))
public class Follow {
    // Surrogate primary key.
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // User who initiated follow.
    @Column(nullable = false) private String followerEmail;
    // User being followed.
    @Column(nullable = false) private String followingEmail;
    // ACTIVE now; can support PENDING if private-account flow is added.
    private String status = "ACTIVE";
    // Creation timestamp for sorting/analytics.
    private LocalDateTime createdAt;

    public Follow() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFollowerEmail() { return followerEmail; }
    public void setFollowerEmail(String followerEmail) { this.followerEmail = followerEmail; }
    public String getFollowingEmail() { return followingEmail; }
    public void setFollowingEmail(String followingEmail) { this.followingEmail = followingEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
