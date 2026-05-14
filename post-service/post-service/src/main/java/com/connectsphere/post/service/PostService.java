package com.connectsphere.post.service;

import com.connectsphere.post.entity.Post;
import java.util.List;

/**
 * <h1>PostService Interface</h1>
 * <p>Manages the core content creation and distribution logic for traditional social media posts.</p>
 * 
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *     <li><b>Content Lifecycle:</b> CRUD operations for user posts, including visibility controls.</li>
 *     <li><b>Engagement Tracking:</b> Optimistic updates for like and comment counters to ensure high-performance UI rendering.</li>
 *     <li><b>Content Discovery:</b> Generating global feeds and supporting keyword-based search functionality.</li>
 * </ul>
 * 
 * <h2>Content Workflow:</h2>
 * <pre>
 * graph TD
 *     A[User] -->|Create| B[PostService]
 *     B -->|Persist| C[(Database)]
 *     C -->|Retrieve| D[User Feed]
 *     C -->|Search| E[Discovery Engine]
 *     A -->|Like/Comment| F[Update Counters]
 *     F --> C
 * </pre>
 */
public interface PostService {

    Post createPost(Post post);

    Post updatePost(Long id, Post post, String userEmail);

    void deletePost(Long id, String userEmail, String userRole);

    Post getPostById(Long id);

    List<Post> getPostsByUser(String userEmail);

    List<Post> getFeed();

    List<Post> searchPosts(String keyword);

    Post updateVisibility(Long id, String visibility, String userEmail);

    void incrementLikes(Long postId);

    void decrementLikes(Long postId);

    void incrementComments(Long postId);

    void decrementComments(Long postId);

    long getPostCount(String userEmail);

    List<Post> getAllPosts();
}
