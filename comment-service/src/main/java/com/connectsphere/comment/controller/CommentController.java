package com.connectsphere.comment.controller;

import com.connectsphere.comment.dto.CommentRequestDto;

import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    // Controller remains thin; validation + persistence rules are in service layer.
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<Comment> addComment(@RequestHeader("X-User-Email") String userEmail,
                                               @Valid @RequestBody CommentRequestDto request) {
        // Supports both top-level comments and replies (via optional parentId in DTO).
        return ResponseEntity.ok(commentService.addComment(userEmail, request));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @GetMapping("/replies/{parentId}")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable Long parentId) {
        return ResponseEntity.ok(commentService.getReplies(parentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> update(@PathVariable Long id,
                                           @RequestHeader("X-User-Email") String userEmail,
                                           @RequestBody String content) {
        // Keeps backward compatibility with plain-text body updates.
        return ResponseEntity.ok(commentService.updateComment(id, userEmail, content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                          @RequestHeader("X-User-Email") String userEmail) {
        commentService.deleteComment(id, userEmail);
        return ResponseEntity.ok("Comment deleted");
    }

    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<Comment>> getByUser(@PathVariable String userEmail) {
        return ResponseEntity.ok(commentService.getCommentsByUser(userEmail));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<String> likeComment(@PathVariable Long id) {
        // Legacy convenience endpoint; main reaction flow can also go through like-service.
        commentService.likeComment(id);
        return ResponseEntity.ok("Comment liked");
    }

    @PostMapping("/{id}/unlike")
    public ResponseEntity<String> unlikeComment(@PathVariable Long id) {
        commentService.unlikeComment(id);
        return ResponseEntity.ok("Comment unliked");
    }

    @GetMapping("/count/{postId}")
    public ResponseEntity<Long> getCount(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentCount(postId));
    }

    @GetMapping("/test")
    public String test() { return "Comment Service is running"; }
}
