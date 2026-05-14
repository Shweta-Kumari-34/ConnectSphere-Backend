package com.connectsphere.like.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import com.connectsphere.like.entity.LikeEntity;
import com.connectsphere.like.exception.ConflictException;
import com.connectsphere.like.exception.ResourceNotFoundException;
import com.connectsphere.like.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * LikeControllerTest — Unit tests for LikeController REST endpoints.
 * Uses MockMvc in standalone mode (no Spring context needed).
 */
@ExtendWith(MockitoExtension.class)
class LikeControllerTest {

    @Mock
    private LikeService likeService;

    @InjectMocks
    private LikeController likeController;

    private MockMvc mockMvc;
    private LikeEntity testLike;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(likeController).build();
        testLike = new LikeEntity();
        testLike.setId(1L);
        testLike.setTargetId(10L);
        testLike.setTargetType("POST");
        testLike.setUserEmail("user@gmail.com");
        testLike.setReactionType("LIKE");
    }

    @Test
    @DisplayName("POST /likes — should like target and return 200")
    void like_Success() throws Exception {
        when(likeService.likeTarget("user@gmail.com", 10L, "POST", "LIKE")).thenReturn(testLike);

        mockMvc.perform(post("/likes")
                        .header("X-User-Email", "user@gmail.com")
                        .param("targetId", "10")
                        .param("targetType", "POST")
                        .param("reactionType", "LIKE"))
                .andExpect(status().isOk());

        verify(likeService).likeTarget("user@gmail.com", 10L, "POST", "LIKE");
    }

    @Test
    @DisplayName("DELETE /likes — should unlike and return 200")
    void unlike_Success() throws Exception {
        doNothing().when(likeService).unlikeTarget("user@gmail.com", 10L, "POST");

        mockMvc.perform(delete("/likes")
                        .header("X-User-Email", "user@gmail.com")
                        .param("targetId", "10")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(content().string("Unliked successfully"));

        verify(likeService).unlikeTarget("user@gmail.com", 10L, "POST");
    }

    @Test
    @DisplayName("GET /likes/has-liked — should return true when liked")
    void hasLiked_True() throws Exception {
        when(likeService.hasLiked("user@gmail.com", 10L, "POST")).thenReturn(true);

        mockMvc.perform(get("/likes/has-liked")
                        .header("X-User-Email", "user@gmail.com")
                        .param("targetId", "10")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /likes/has-liked — should return false when not liked")
    void hasLiked_False() throws Exception {
        when(likeService.hasLiked("user@gmail.com", 10L, "POST")).thenReturn(false);

        mockMvc.perform(get("/likes/has-liked")
                        .header("X-User-Email", "user@gmail.com")
                        .param("targetId", "10")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("GET /likes/target/{targetId} — should return likes list")
    void getByTarget_Success() throws Exception {
        when(likeService.getLikesByTarget(10L, "POST")).thenReturn(List.of(testLike));

        mockMvc.perform(get("/likes/target/10")
                        .param("targetType", "POST"))
                .andExpect(status().isOk());

        verify(likeService).getLikesByTarget(10L, "POST");
    }

    @Test
    @DisplayName("GET /likes/user/{userEmail} — should return user's likes")
    void getByUser_Success() throws Exception {
        when(likeService.getLikesByUser("user@gmail.com")).thenReturn(List.of(testLike));

        mockMvc.perform(get("/likes/user/user@gmail.com"))
                .andExpect(status().isOk());

        verify(likeService).getLikesByUser("user@gmail.com");
    }

    @Test
    @DisplayName("GET /likes/count/{targetId} — should return like count")
    void getCount_Success() throws Exception {
        when(likeService.getLikeCount(10L, "POST")).thenReturn(42L);

        mockMvc.perform(get("/likes/count/10")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    @DisplayName("PUT /likes/change-reaction — should update reaction type")
    void changeReaction_Success() throws Exception {
        testLike.setReactionType("LOVE");
        when(likeService.changeReaction("user@gmail.com", 10L, "POST", "LOVE")).thenReturn(testLike);

        mockMvc.perform(put("/likes/change-reaction")
                        .header("X-User-Email", "user@gmail.com")
                        .param("targetId", "10")
                        .param("targetType", "POST")
                        .param("reactionType", "LOVE"))
                .andExpect(status().isOk());

        verify(likeService).changeReaction("user@gmail.com", 10L, "POST", "LOVE");
    }

    @Test
    @DisplayName("GET /likes/summary/{targetId} — should return reaction summary")
    void getReactionSummary_Success() throws Exception {
        when(likeService.getReactionSummary(10L, "POST")).thenReturn(Map.of("LIKE", 5L, "LOVE", 2L));

        mockMvc.perform(get("/likes/summary/10")
                        .param("targetType", "POST"))
                .andExpect(status().isOk());

        verify(likeService).getReactionSummary(10L, "POST");
    }

    @Test
    @DisplayName("GET /likes/count-by-type/{targetId} — should return count by type")
    void getCountByType_Success() throws Exception {
        when(likeService.getLikeCountByType(10L, "POST", "LIKE")).thenReturn(7L);

        mockMvc.perform(get("/likes/count-by-type/10")
                        .param("targetType", "POST")
                        .param("reactionType", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    @DisplayName("GET /likes/test — should return health check message")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/likes/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Like Service is running"));
    }
}
