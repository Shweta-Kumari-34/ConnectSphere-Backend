package com.connectsphere.post.controller;

import com.connectsphere.post.dto.ReelCommentRequestDto;
import com.connectsphere.post.entity.Reel;
import com.connectsphere.post.entity.ReelComment;
import com.connectsphere.post.service.ReelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reels")
public class ReelController {

    private final ReelService reelService;

    public ReelController(ReelService reelService) {
        this.reelService = reelService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadReel(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false, defaultValue = "PUBLIC") String visibility
    ) {
        try {
            System.out.println("\n========== REEL UPLOAD ==========");
            System.out.println("USER EMAIL : " + userEmail);
            System.out.println("FILE NAME  : " + file.getOriginalFilename());
            System.out.println("FILE SIZE  : " + file.getSize());
            System.out.println("CAPTION    : " + caption);
            System.out.println("VISIBILITY : " + visibility);
            System.out.println("================================");

            Reel reel = reelService.uploadReel(userEmail, file, caption, visibility);
            return ResponseEntity.ok(reel);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/feed/{userEmail}")
    public ResponseEntity<List<Reel>> getFeed(@PathVariable String userEmail) {
        return ResponseEntity.ok(reelService.getFeedForUser(userEmail));
    }

    @GetMapping("/my-reels/{userEmail}")
    public ResponseEntity<List<Reel>> getMyReels(@PathVariable String userEmail) {
        return ResponseEntity.ok(reelService.getUserReels(userEmail));
    }

    @GetMapping("/{reelId}")
    public ResponseEntity<Reel> getReelById(@PathVariable Long reelId) {
        return ResponseEntity.ok(reelService.getReelById(reelId));
    }

    @DeleteMapping("/{reelId}")
    public ResponseEntity<String> deleteReel(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long reelId
    ) {
        reelService.deleteReel(userEmail, userRole, reelId);
        return ResponseEntity.ok("Reel deleted successfully");
    }

    // Backward-compatible delete routes used by older frontend builds
    @DeleteMapping("/{reelId}/delete")
    public ResponseEntity<String> deleteReelLegacyPath(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long reelId
    ) {
        reelService.deleteReel(userEmail, userRole, reelId);
        return ResponseEntity.ok("Reel deleted successfully");
    }

    @DeleteMapping("/delete/{reelId}")
    public ResponseEntity<String> deleteReelLegacyPrefix(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long reelId
    ) {
        reelService.deleteReel(userEmail, userRole, reelId);
        return ResponseEntity.ok("Reel deleted successfully");
    }

    @PostMapping("/{reelId}/comments")
    public ResponseEntity<ReelComment> addComment(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @PathVariable Long reelId,
            @RequestBody ReelCommentRequestDto request
    ) {
        return ResponseEntity.ok(reelService.addComment(userEmail, reelId, request.getContent()));
    }

    @GetMapping("/{reelId}/comments")
    public ResponseEntity<List<ReelComment>> getComments(@PathVariable Long reelId) {
        return ResponseEntity.ok(reelService.getComments(reelId));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(403).body(e.getMessage());
    }
}
