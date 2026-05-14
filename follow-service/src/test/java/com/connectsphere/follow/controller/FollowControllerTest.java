package com.connectsphere.follow.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.service.FollowService;
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

/**
 * FollowControllerTest — Unit tests for FollowController REST endpoints.
 * Uses standalone MockMvc — no Spring context or DB required.
 */
@ExtendWith(MockitoExtension.class)
class FollowControllerTest {

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowController followController;

    private MockMvc mockMvc;
    private Follow testFollow;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(followController).build();
        testFollow = new Follow();
        testFollow.setId(1L);
        testFollow.setFollowerEmail("user1@gmail.com");
        testFollow.setFollowingEmail("user2@gmail.com");
    }

    @Test
    @DisplayName("POST /follows — should follow user and return 200")
    void follow_Success() throws Exception {
        when(followService.follow("user1@gmail.com", "user2@gmail.com")).thenReturn(testFollow);

        mockMvc.perform(post("/follows")
                        .header("X-User-Email", "user1@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followingEmail\":\"user2@gmail.com\"}"))
                .andExpect(status().isOk());

        verify(followService).follow("user1@gmail.com", "user2@gmail.com");
    }

    @Test
    @DisplayName("DELETE /follows/{followingEmail} — should unfollow and return 200")
    void unfollow_Success() throws Exception {
        doNothing().when(followService).unfollow("user1@gmail.com", "user2@gmail.com");

        mockMvc.perform(delete("/follows/user2@gmail.com")
                        .header("X-User-Email", "user1@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Unfollowed successfully"));
    }

    @Test
    @DisplayName("GET /follows/is-following — should return true when following")
    void isFollowing_True() throws Exception {
        when(followService.isFollowing("user1@gmail.com", "user2@gmail.com")).thenReturn(true);

        mockMvc.perform(get("/follows/is-following")
                        .header("X-User-Email", "user1@gmail.com")
                        .param("followingEmail", "user2@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /follows/is-following — should return false when not following")
    void isFollowing_False() throws Exception {
        when(followService.isFollowing("user1@gmail.com", "user2@gmail.com")).thenReturn(false);

        mockMvc.perform(get("/follows/is-following")
                        .header("X-User-Email", "user1@gmail.com")
                        .param("followingEmail", "user2@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("GET /follows/followers — should return current user's followers")
    void getMyFollowers_Success() throws Exception {
        when(followService.getFollowers("user1@gmail.com")).thenReturn(List.of(testFollow));

        mockMvc.perform(get("/follows/followers")
                        .header("X-User-Email", "user1@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /follows/followers/{userEmail} — should return any user's followers")
    void getFollowers_Success() throws Exception {
        when(followService.getFollowers("user2@gmail.com")).thenReturn(List.of(testFollow));

        mockMvc.perform(get("/follows/followers/user2@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /follows/following — should return current user's following")
    void getMyFollowing_Success() throws Exception {
        when(followService.getFollowing("user1@gmail.com")).thenReturn(List.of(testFollow));

        mockMvc.perform(get("/follows/following")
                        .header("X-User-Email", "user1@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /follows/following/{userEmail} — should return any user's following")
    void getFollowing_Success() throws Exception {
        when(followService.getFollowing("user2@gmail.com")).thenReturn(List.of(testFollow));

        mockMvc.perform(get("/follows/following/user2@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /follows/followers/count — should return follower count")
    void getMyFollowerCount_Success() throws Exception {
        when(followService.getFollowerCount("user1@gmail.com")).thenReturn(5L);

        mockMvc.perform(get("/follows/followers/count")
                        .header("X-User-Email", "user1@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("GET /follows/following/count — should return following count")
    void getMyFollowingCount_Success() throws Exception {
        when(followService.getFollowingCount("user1@gmail.com")).thenReturn(3L);

        mockMvc.perform(get("/follows/following/count")
                        .header("X-User-Email", "user1@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @DisplayName("GET /follows/follower-count/{userEmail} — should return count for any user")
    void getFollowerCount_Success() throws Exception {
        when(followService.getFollowerCount("user2@gmail.com")).thenReturn(10L);

        mockMvc.perform(get("/follows/follower-count/user2@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    @DisplayName("GET /follows/following-count/{userEmail} — should return following count for any user")
    void getFollowingCount_Success() throws Exception {
        when(followService.getFollowingCount("user2@gmail.com")).thenReturn(7L);

        mockMvc.perform(get("/follows/following-count/user2@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    @DisplayName("GET /follows/mutual — should return mutual follows")
    void getMutualFollows_Success() throws Exception {
        when(followService.getMutualFollows("user1@gmail.com", "user2@gmail.com"))
                .thenReturn(List.of("common@gmail.com"));

        mockMvc.perform(get("/follows/mutual")
                        .header("X-User-Email", "user1@gmail.com")
                        .param("otherEmail", "user2@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /follows/suggested — should return suggested users")
    void getSuggestedUsers_Success() throws Exception {
        when(followService.getSuggestedUsers("user1@gmail.com")).thenReturn(List.of("suggested@gmail.com"));

        mockMvc.perform(get("/follows/suggested")
                        .header("X-User-Email", "user1@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /follows/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/follows/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Follow Service is running"));
    }
}
