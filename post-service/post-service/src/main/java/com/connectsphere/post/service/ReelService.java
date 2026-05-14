package com.connectsphere.post.service;

import com.connectsphere.post.entity.Reel;
import com.connectsphere.post.entity.ReelComment;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * <h1>ReelService Interface</h1>
 * <p>Manages short-form video content (Reels), designed for high-engagement, vertical video feeds.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *     <li><b>Media Handling:</b> Orchestrating physical video uploads and generating accessible media URLs.</li>
 *     <li><b>Feed Generation:</b> Serving algorithmic or chronological video feeds for users.</li>
 *     <li><b>Nested Engagement:</b> Managing direct comments and replies specific to short-form content.</li>
 * </ul>
 * 
 * <h2>Reel Processing Flow:</h2>
 * <pre>
 * graph LR
 *     A[User Upload] -->|MultipartFile| B{File System}
 *     B -->|Path/URL| C[Database Persistence]
 *     C -->|Serve| D[Infinite Scroll Feed]
 *     D -->|Engage| E[Add Comment]
 * </pre>
 */
public interface ReelService {
    Reel uploadReel(String userEmail, MultipartFile file, String caption, String visibility);
    List<Reel> getFeedForUser(String userEmail);
    List<Reel> getUserReels(String userEmail);
    Reel getReelById(Long reelId);
    void deleteReel(String userEmail, String userRole, Long reelId);
    ReelComment addComment(String userEmail, Long reelId, String content);
    List<ReelComment> getComments(Long reelId);
}
