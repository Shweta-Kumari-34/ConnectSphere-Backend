package com.connectsphere.follow.controller;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * FollowController
 * ----------------
 * REST endpoints for follow-service.
 *
 * Available APIs:
 *   POST   /follows                          → Follow a user (secured)
 *   DELETE /follows/{followingEmail}          → Unfollow a user (secured)
 *   GET    /follows/is-following?followingEmail=... → Check follow status (secured)
 *   GET    /follows/followers                → Get current user's followers (secured)
 *   GET    /follows/followers/{userEmail}    → Get any user's followers
 *   GET    /follows/following                → Get current user's following (secured)
 *   GET    /follows/following/{userEmail}    → Get any user's following
 *   GET    /follows/followers/count          → Current user's follower count (secured)
 *   GET    /follows/following/count          → Current user's following count (secured)
 *   GET    /follows/follower-count/{userEmail} → Any user's follower count
 *   GET    /follows/following-count/{userEmail} → Any user's following count
 *   GET    /follows/test                     → Health check (public)
 *
 * Secured endpoints receive user email via "X-User-Email" header
 * which is injected by the API Gateway's JwtFilter.
 */
@RestController
@RequestMapping("/follows")
public class FollowController {

    // All follow graph operations are delegated to FollowService.
    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    // POST /follows — Follow a user (accepts JSON body from Angular)
    @PostMapping
    public ResponseEntity<Follow> follow(
            @RequestHeader("X-User-Email") String follower,
            @RequestBody Map<String, String> body) {
        // Request body shape: { "followingEmail": "target@domain.com" }.
        String followingEmail = body.get("followingEmail");
        return ResponseEntity.ok(followService.follow(follower, followingEmail));
    }

    // DELETE /follows/{followingEmail} — Unfollow a user
    @DeleteMapping("/{followingEmail}")
    public ResponseEntity<String> unfollow(
            @RequestHeader("X-User-Email") String follower,
            @PathVariable String followingEmail) {
        followService.unfollow(follower, followingEmail);
        return ResponseEntity.ok("Unfollowed successfully");
    }

    // GET /follows/is-following?followingEmail=... — Check if following
    @GetMapping("/is-following")
    public ResponseEntity<Boolean> isFollowing(
            @RequestHeader("X-User-Email") String follower,
            @RequestParam String followingEmail) {
        return ResponseEntity.ok(followService.isFollowing(follower, followingEmail));
    }

    // GET /follows/followers — Current user's followers (header-based)
    @GetMapping("/followers")
    public ResponseEntity<List<Follow>> getMyFollowers(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(followService.getFollowers(userEmail));
    }

    // GET /follows/followers/{userEmail} — Any user's followers
    @GetMapping("/followers/{userEmail}")
    public ResponseEntity<List<Follow>> getFollowers(@PathVariable String userEmail) {
        return ResponseEntity.ok(followService.getFollowers(userEmail));
    }

    // GET /follows/following — Current user's following (header-based)
    @GetMapping("/following")
    public ResponseEntity<List<Follow>> getMyFollowing(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(followService.getFollowing(userEmail));
    }

    // GET /follows/following/{userEmail} — Any user's following
    @GetMapping("/following/{userEmail}")
    public ResponseEntity<List<Follow>> getFollowing(@PathVariable String userEmail) {
        return ResponseEntity.ok(followService.getFollowing(userEmail));
    }

    // GET /follows/followers/count — Current user's follower count (header-based)
    @GetMapping("/followers/count")
    public ResponseEntity<Long> getMyFollowerCount(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(followService.getFollowerCount(userEmail));
    }

    // GET /follows/following/count — Current user's following count (header-based)
    @GetMapping("/following/count")
    public ResponseEntity<Long> getMyFollowingCount(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(followService.getFollowingCount(userEmail));
    }

    // GET /follows/follower-count/{userEmail} — Any user's follower count
    @GetMapping("/follower-count/{userEmail}")
    public ResponseEntity<Long> getFollowerCount(@PathVariable String userEmail) {
        return ResponseEntity.ok(followService.getFollowerCount(userEmail));
    }

    // GET /follows/following-count/{userEmail} — Any user's following count
    @GetMapping("/following-count/{userEmail}")
    public ResponseEntity<Long> getFollowingCount(@PathVariable String userEmail) {
        return ResponseEntity.ok(followService.getFollowingCount(userEmail));
    }

    // GET /follows/mutual?otherEmail=... — Mutual follows between current user and another
    @GetMapping("/mutual")
    public ResponseEntity<List<String>> getMutualFollows(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestParam String otherEmail) {
        // Returns shared connections between current user and otherEmail.
        return ResponseEntity.ok(followService.getMutualFollows(userEmail, otherEmail));
    }

    // GET /follows/suggested — Suggested users to follow (second-degree connections)
    @GetMapping("/suggested")
    public ResponseEntity<List<String>> getSuggestedUsers(
            @RequestHeader("X-User-Email") String userEmail) {
        // Suggestion logic is based on second-degree follow relationships.
        return ResponseEntity.ok(followService.getSuggestedUsers(userEmail));
    }

    // GET /follows/test — Health check
    @GetMapping("/test")
    public String test() {
        return "Follow Service is running";
    }
}
