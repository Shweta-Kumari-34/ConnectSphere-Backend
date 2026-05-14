package com.connectsphere.post.controller;

import com.connectsphere.post.entity.Post;
import com.connectsphere.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * PostController
 * --------------
 * REST endpoints for post-service matching the case study.
 *
 * Available APIs:
 *   POST   /posts                    → Create a new post
 *   GET    /posts/{id}               → Get post by ID
 *   GET    /posts/user/{userEmail}   → Get posts by user
 *   GET    /posts/feed               → Get feed (all posts sorted by date)
 *   PUT    /posts/{id}               → Update a post
 *   DELETE /posts/{id}               → Delete a post
 *   GET    /posts/search?q=keyword   → Search posts
 *   PUT    /posts/{id}/visibility    → Change visibility
 *   GET    /posts/count/{userEmail}  → Post count for a user
 *   GET    /posts/all                → Get all posts (legacy)
 *   GET    /posts/test               → Health check
 *
 * Secured endpoints receive user email via "X-User-Email" header
 * which is injected by the API Gateway's JwtFilter.
 */
@RestController
@RequestMapping("/posts")
public class PostController {

    // Delegates all business rules to service layer.
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // POST /posts — Create a new post
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post,
                                           @RequestHeader("X-User-Email") String userEmail) {
        // Author identity is taken from gateway header, never from client payload.
        post.setUserEmail(userEmail);
        Post savedPost = postService.createPost(post);
        return ResponseEntity.ok(savedPost);
    }

    // POST /posts/create — Create post (legacy endpoint)
    @PostMapping("/create")
    public ResponseEntity<Post> createPostLegacy(@RequestBody Post post,
                                                  @RequestHeader("X-User-Email") String userEmail) {
        post.setUserEmail(userEmail);
        Post savedPost = postService.createPost(post);
        return ResponseEntity.ok(savedPost);
    }

    // GET /posts/{id} — Get post by ID
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Post post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    // GET /posts/user/{userEmail} — Get posts by user
    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<Post>> getPostsByUser(@PathVariable String userEmail) {
        List<Post> posts = postService.getPostsByUser(userEmail);
        return ResponseEntity.ok(posts);
    }

    // GET /posts/feed — Get feed (all public posts sorted by date)
    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed() {
        List<Post> feed = postService.getFeed();
        return ResponseEntity.ok(feed);
    }

    // PUT /posts/{id} — Update a post
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id,
                                           @RequestBody Post post,
                                           @RequestHeader("X-User-Email") String userEmail) {
        // Service enforces ownership/authorization checks.
        Post updated = postService.updatePost(id, post, userEmail);
        return ResponseEntity.ok(updated);
    }

    // DELETE /posts/{id} — Delete a post
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long id,
                                             @RequestHeader("X-User-Email") String userEmail,
                                             @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        postService.deletePost(id, userEmail, userRole);
        return ResponseEntity.ok("Post deleted successfully");
    }

    // GET /posts/search?q=keyword — Search posts
    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String q) {
        List<Post> results = postService.searchPosts(q);
        return ResponseEntity.ok(results);
    }

    // PUT /posts/{id}/visibility — Change visibility
    @PutMapping("/{id}/visibility")
    public ResponseEntity<Post> updateVisibility(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body,
                                                  @RequestHeader("X-User-Email") String userEmail) {
        String visibility = body.get("visibility");
        Post updated = postService.updateVisibility(id, visibility, userEmail);
        return ResponseEntity.ok(updated);
    }

    // GET /posts/count/{userEmail} — Post count for a user
    @GetMapping("/count/{userEmail}")
    public ResponseEntity<Map<String, Object>> getPostCount(@PathVariable String userEmail) {
        long count = postService.getPostCount(userEmail);
        return ResponseEntity.ok(Map.of("userEmail", userEmail, "count", count));
    }

    // GET /posts/all — Get all posts (legacy)
    @GetMapping("/all")
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }
    
    @GetMapping
    public ResponseEntity<List<Post>> getAllPostsDefault() {
        // Default GET /posts kept for compatibility with generic clients.
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // GET /posts/my — Get my posts (legacy)
    @GetMapping("/my")
    public ResponseEntity<List<Post>> getMyPosts(@RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(postService.getPostsByUser(userEmail));
    }

    // GET /posts/test — Health check
    @GetMapping("/test")
    public String test() {
        return "Post Service is running!";
    }
}
