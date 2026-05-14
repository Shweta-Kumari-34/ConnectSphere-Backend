package com.connectsphere.post.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.post.client.MediaServiceClient;
import com.connectsphere.post.client.SearchServiceClient;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.producer.NotificationEventProducer;
import com.connectsphere.post.exception.ResourceNotFoundException;
import com.connectsphere.post.exception.UnauthorizedException;
import com.connectsphere.post.service.impl.PostServiceImpl;

/*
 * PostServiceTest
 * ---------------
 * Unit tests for PostServiceImpl using JUnit 5 + Mockito.
 *
 * Tests:
 *   1. createPost — save and return
 *   2. getAllPosts — return all
 *   3. getPostsByUser — filtered by email
 *   4. getPostById — success + not found
 *   5. deletePost — success + unauthorized
 *   6. searchPosts — keyword search
 *   7. getPostCount — count by user
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MediaServiceClient mediaServiceClient;

    @Mock
    private SearchServiceClient searchServiceClient;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private PostServiceImpl postService;

    private Post testPost1;
    private Post testPost2;

    @BeforeEach
    void setUp() {
        testPost1 = new Post();
        testPost1.setId(1L);
        testPost1.setTitle("First Post");
        testPost1.setContent("Hello World");
        testPost1.setUserEmail("user1@gmail.com");

        testPost2 = new Post();
        testPost2.setId(2L);
        testPost2.setTitle("Second Post");
        testPost2.setContent("Another post");
        testPost2.setUserEmail("user2@gmail.com");
    }

    @Test
    @DisplayName("CreatePost - should save and return the post")
    void createPost_Success() {
        when(postRepository.save(any(Post.class))).thenReturn(testPost1);

        Post result = postService.createPost(testPost1);

        assertNotNull(result);
        assertEquals("First Post", result.getTitle());
        assertEquals("user1@gmail.com", result.getUserEmail());
        verify(postRepository).save(testPost1);
        verify(searchServiceClient).indexPost(testPost1.getId(), testPost1.getContent());
    }

    @Test
    @DisplayName("GetAllPosts - should return all posts")
    void getAllPosts_Success() {
        when(postRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(Arrays.asList(testPost1, testPost2));

        List<Post> result = postService.getAllPosts();

        assertEquals(2, result.size());
        verify(postRepository).findAllByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("GetAllPosts - should return empty list when no posts exist")
    void getAllPosts_Empty() {
        when(postRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());

        List<Post> result = postService.getAllPosts();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GetPostsByUser - should return only user's posts")
    void getPostsByUser_Success() {
        when(postRepository.findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc("user1@gmail.com"))
                .thenReturn(List.of(testPost1));

        List<Post> result = postService.getPostsByUser("user1@gmail.com");

        assertEquals(1, result.size());
        assertEquals("user1@gmail.com", result.get(0).getUserEmail());
    }

    @Test
    @DisplayName("GetPostById - should return post when found")
    void getPostById_Success() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPost1));

        Post result = postService.getPostById(1L);

        assertNotNull(result);
        assertEquals("First Post", result.getTitle());
    }

    @Test
    @DisplayName("GetPostById - should throw when post not found")
    void getPostById_NotFound() {
        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(99L));
    }

    @Test
    @DisplayName("DeletePost - should throw when not owner")
    void deletePost_Unauthorized() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPost1));

        assertThrows(UnauthorizedException.class,
                () -> postService.deletePost(1L, "hacker@gmail.com", "USER"));
        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DeletePost - should soft-delete post and cascade media cleanup")
    void deletePost_Success() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPost1));

        postService.deletePost(1L, "user1@gmail.com", "USER");

        assertTrue(testPost1.isDeleted());
        verify(postRepository).save(testPost1);
        verify(searchServiceClient).removePostIndex(1L);
        verify(mediaServiceClient).softDeleteMediaByPost(1L);
    }

    @Test
    @DisplayName("DeletePost - admin should delete non-owned post")
    void deletePost_AdminCanDeleteOthersPost() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPost1));

        postService.deletePost(1L, "admin@gmail.com", "ADMIN");

        assertTrue(testPost1.isDeleted());
        verify(postRepository).save(testPost1);
        verify(searchServiceClient).removePostIndex(1L);
        verify(mediaServiceClient).softDeleteMediaByPost(1L);
    }

    @Test
    @DisplayName("DeletePost - ROLE_ADMIN should also delete non-owned post")
    void deletePost_RoleAdminCanDeleteOthersPost() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPost1));

        postService.deletePost(1L, "admin@gmail.com", "ROLE_ADMIN");

        assertTrue(testPost1.isDeleted());
        verify(postRepository).save(testPost1);
        verify(searchServiceClient).removePostIndex(1L);
        verify(mediaServiceClient).softDeleteMediaByPost(1L);
    }

    @Test
    @DisplayName("SearchPosts - should search by keyword")
    void searchPosts_Success() {
        when(postRepository.searchByContent("Hello")).thenReturn(List.of(testPost1));

        List<Post> result = postService.searchPosts("Hello");

        assertEquals(1, result.size());
        assertEquals("Hello World", result.get(0).getContent());
    }

    @Test
    @DisplayName("GetPostCount - should return count for user")
    void getPostCount_Success() {
        when(postRepository.countByUserEmailAndIsDeletedFalse("user1@gmail.com")).thenReturn(5L);

        long count = postService.getPostCount("user1@gmail.com");

        assertEquals(5L, count);
    }
}
