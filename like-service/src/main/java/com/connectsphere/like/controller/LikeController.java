package com.connectsphere.like.controller;

import com.connectsphere.like.entity.LikeEntity;
import com.connectsphere.like.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * <h1>LikeController</h1>
 * <p>REST API endpoints for managing user engagements and reactions across multiple target 
 * types within the ConnectSphere ecosystem.</p>
 * 
 * <h2>API Request Flow:</h2>
 * <pre>
 * graph TD
 *     A[Frontend Client] -->|HTTP POST /likes| B(API Gateway)
 *     B -->|Injects X-User-Email| C[LikeController]
 *     C -->|Delegates Logic| D{LikeService}
 *     D -->|Persists & Notifies| E[(Database / Message Queue)]
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Polymorphism:</b> Handles reactions for POST, COMMENT, STORY, and REEL using dynamic type parameters.</li>
 *     <li><b>Gateway Security:</b> Relies on {@code X-User-Email} headers securely injected by the API Gateway.</li>
 *     <li><b>Reaction Diversity:</b> Extends basic likes to support diverse emotional expressions (e.g., LOVE, HAHA).</li>
 * </ul>
 */
@RestController
@RequestMapping("/likes")
public class LikeController {

    // Centralized engagement logic handling database interaction and caching.
    private final LikeService likeService;
    
    public LikeController(LikeService likeService) { 
        this.likeService = likeService; 
    }

    /**
     * Endpoint to add a new reaction to a specific target.
     * Inherits the target's type dynamically from the request parameters.
     */
    @PostMapping
    public ResponseEntity<LikeEntity> like(@RequestHeader("X-User-Email") String userEmail,
                                            @RequestParam Long targetId, @RequestParam String targetType,
                                            @RequestParam(required = false) String reactionType) {
        return ResponseEntity.ok(likeService.likeTarget(userEmail, targetId, targetType, reactionType));
    }

    // Removes current user's like/reaction from target.
    @DeleteMapping
    public ResponseEntity<String> unlike(@RequestHeader("X-User-Email") String userEmail,
                                          @RequestParam Long targetId, @RequestParam String targetType) {
        likeService.unlikeTarget(userEmail, targetId, targetType);
        return ResponseEntity.ok("Unliked successfully");
    }

    // Checks whether current user has already liked the target.
    @GetMapping("/has-liked")
    public ResponseEntity<Boolean> hasLiked(@RequestHeader("X-User-Email") String userEmail,
                                             @RequestParam Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(likeService.hasLiked(userEmail, targetId, targetType));
    }

    @GetMapping("/target/{targetId}")
    public ResponseEntity<List<LikeEntity>> getByTarget(@PathVariable Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(likeService.getLikesByTarget(targetId, targetType));
    }

    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<LikeEntity>> getByUser(@PathVariable String userEmail) {
        return ResponseEntity.ok(likeService.getLikesByUser(userEmail));
    }

    // Returns aggregated like count for a target.
    @GetMapping("/count/{targetId}")
    public ResponseEntity<Long> getCount(@PathVariable Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(likeService.getLikeCount(targetId, targetType));
    }

    // Changes reaction type (e.g., LIKE -> LOVE) without removing old record manually.
    @PutMapping("/change-reaction")
    public ResponseEntity<LikeEntity> changeReaction(@RequestHeader("X-User-Email") String userEmail,
                                                      @RequestParam Long targetId, @RequestParam String targetType,
                                                      @RequestParam String reactionType) {
        return ResponseEntity.ok(likeService.changeReaction(userEmail, targetId, targetType, reactionType));
    }

    @GetMapping("/summary/{targetId}")
    public ResponseEntity<Map<String, Long>> getReactionSummary(@PathVariable Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(likeService.getReactionSummary(targetId, targetType));
    }

    @GetMapping("/count-by-type/{targetId}")
    public ResponseEntity<Long> getCountByType(@PathVariable Long targetId, @RequestParam String targetType,
                                                @RequestParam String reactionType) {
        return ResponseEntity.ok(likeService.getLikeCountByType(targetId, targetType, reactionType));
    }

    @GetMapping("/test")
    public String test() { return "Like Service is running"; }
}
