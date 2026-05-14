package com.connectsphere.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import com.connectsphere.post.entity.Post;
import com.connectsphere.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private MockMvc mockMvc;
    private Post testPost;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
        testPost = new Post();
        testPost.setId(1L);
        testPost.setUserEmail("user@gmail.com");
        testPost.setContent("Hello ConnectSphere!");
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setVisibility("PUBLIC");
    }

    @Test
    @DisplayName("POST /posts — should create post")
    void createPost_Success() throws Exception {
        when(postService.createPost(any(Post.class))).thenReturn(testPost);

        mockMvc.perform(post("/posts")
                        .header("X-User-Email", "user@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello ConnectSphere!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /posts/create — should create post via legacy endpoint")
    void createPostLegacy_Success() throws Exception {
        when(postService.createPost(any(Post.class))).thenReturn(testPost);

        mockMvc.perform(post("/posts/create")
                        .header("X-User-Email", "user@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello ConnectSphere!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/{id} — should return post by id")
    void getPostById_Success() throws Exception {
        when(postService.getPostById(1L)).thenReturn(testPost);

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/user/{userEmail} — should return posts by user")
    void getPostsByUser_Success() throws Exception {
        when(postService.getPostsByUser("user@gmail.com")).thenReturn(List.of(testPost));

        mockMvc.perform(get("/posts/user/user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/feed — should return feed")
    void getFeed_Success() throws Exception {
        when(postService.getFeed()).thenReturn(List.of(testPost));

        mockMvc.perform(get("/posts/feed"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /posts/{id} — should update post")
    void updatePost_Success() throws Exception {
        testPost.setContent("Updated content");
        when(postService.updatePost(eq(1L), any(Post.class), eq("user@gmail.com"))).thenReturn(testPost);

        mockMvc.perform(put("/posts/1")
                        .header("X-User-Email", "user@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated content\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /posts/{id} — should delete post")
    void deletePost_Success() throws Exception {
        doNothing().when(postService).deletePost(1L, "user@gmail.com", null);

        mockMvc.perform(delete("/posts/1")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Post deleted successfully"));
    }

    @Test
    @DisplayName("GET /posts/search — should return matching posts")
    void searchPosts_Success() throws Exception {
        when(postService.searchPosts("Hello")).thenReturn(List.of(testPost));

        mockMvc.perform(get("/posts/search").param("q", "Hello"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /posts/{id}/visibility — should update visibility")
    void updateVisibility_Success() throws Exception {
        when(postService.updateVisibility(1L, "PRIVATE", "user@gmail.com")).thenReturn(testPost);

        mockMvc.perform(put("/posts/1/visibility")
                        .header("X-User-Email", "user@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/count/{userEmail} — should return post count")
    void getPostCount_Success() throws Exception {
        when(postService.getPostCount("user@gmail.com")).thenReturn(5L);

        mockMvc.perform(get("/posts/count/user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/all — should return all posts")
    void getAllPosts_Success() throws Exception {
        when(postService.getAllPosts()).thenReturn(List.of(testPost));

        mockMvc.perform(get("/posts/all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/my — should return my posts")
    void getMyPosts_Success() throws Exception {
        when(postService.getPostsByUser("user@gmail.com")).thenReturn(List.of(testPost));

        mockMvc.perform(get("/posts/my")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/posts/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Post Service is running!"));
    }
}
