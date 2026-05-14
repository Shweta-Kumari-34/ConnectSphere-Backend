package com.connectsphere.search.controller;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * SearchController — REST API for search and hashtag operations.
 * Matches case study §4.8 SearchResource class diagram.
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    // Delegates indexing/search computation to SearchService.
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    // POST /search/index — Index hashtags from post content (auto-extract #tags)
    @PostMapping("/index")
    public ResponseEntity<String> indexPost(@RequestBody Map<String, Object> request) {
        // Supports two payload shapes:
        // 1) {postId, content} -> extract hashtags from content
        // 2) {postId, tag}     -> index a single hashtag manually
        Long postId = Long.valueOf(request.get("postId").toString());
        String content = (String) request.get("content");
        if (content != null) {
            searchService.indexPost(postId, content);
        } else {
            String tag = (String) request.get("tag");
            searchService.indexHashtag(tag, postId);
        }
        return ResponseEntity.ok("Indexed successfully");
    }

    // DELETE /search/index/{postId} — Remove all hashtags for a post
    @DeleteMapping("/index/{postId}")
    public ResponseEntity<String> removePostIndex(@PathVariable Long postId) {
        searchService.removePostIndex(postId);
        return ResponseEntity.ok("Post index removed");
    }

    // GET /search/posts?q=... — Search posts by keyword/hashtag
    @GetMapping("/posts")
    public ResponseEntity<List<Long>> searchPosts(@RequestParam String q) {
        return ResponseEntity.ok(searchService.searchPosts(q));
    }

    // GET /search/users?q=... — Search users by keyword
    @GetMapping("/users")
    public ResponseEntity<List<String>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(searchService.searchUsers(q));
    }

    // GET /search/hashtags/post/{postId} — Get hashtags for a post
    @GetMapping("/hashtags/post/{postId}")
    public ResponseEntity<List<Hashtag>> getHashtagsForPost(@PathVariable Long postId) {
        return ResponseEntity.ok(searchService.getHashtagsForPost(postId));
    }

    // GET /search/trending?limit=10 — Trending hashtags
    @GetMapping("/trending")
    public ResponseEntity<List<Object[]>> getTrending(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.getTrendingHashtags(limit));
    }

    // GET /search/posts-by-hashtag?tag=... — Get post IDs by exact hashtag
    @GetMapping("/posts-by-hashtag")
    public ResponseEntity<List<Long>> getPostsByHashtag(@RequestParam String tag) {
        return ResponseEntity.ok(searchService.getPostsByHashtag(tag));
    }

    // GET /search/hashtags?q=... — Search hashtags by keyword
    @GetMapping("/hashtags")
    public ResponseEntity<List<Hashtag>> searchHashtags(@RequestParam String q) {
        return ResponseEntity.ok(searchService.searchHashtags(q));
    }

    // GET /search/count?tag=... — Hashtag usage count
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount(@RequestParam String tag) {
        long count = searchService.getHashtagCount(tag);
        return ResponseEntity.ok(Map.of("tag", tag, "count", count));
    }

    @GetMapping("/test")
    public String test() { return "Search Service is running!"; }
}
