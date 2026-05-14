package com.connectsphere.media.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    private MockMvc mockMvc;
    private Media testMedia;
    private Story testStory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController).build();

        testMedia = new Media();
        testMedia.setId(1L);
        testMedia.setUserEmail("user@gmail.com");
        testMedia.setPostId(10L);
        testMedia.setMediaUrl("https://example.com/photo.jpg");
        testMedia.setMediaType("IMAGE");

        testStory = new Story();
        testStory.setId(1L);
        testStory.setUserEmail("user@gmail.com");
        testStory.setMediaUrl("https://example.com/story.jpg");
        testStory.setActive(true);
    }

    @Test
    @DisplayName("POST /media/upload — should upload media by URL")
    void upload_Success() throws Exception {
        when(mediaService.uploadMedia("user@gmail.com", 10L, "https://example.com/photo.jpg", "IMAGE"))
                .thenReturn(testMedia);

        mockMvc.perform(post("/media/upload")
                        .header("X-User-Email", "user@gmail.com")
                        .param("postId", "10")
                        .param("mediaUrl", "https://example.com/photo.jpg")
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /media/post/{postId} — should return media list for post")
    void getByPost_Success() throws Exception {
        when(mediaService.getMediaByPost(10L)).thenReturn(List.of(testMedia));

        mockMvc.perform(get("/media/post/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /media/{id} — should return media by id")
    void getById_Success() throws Exception {
        when(mediaService.getMediaById(1L)).thenReturn(Optional.of(testMedia));

        mockMvc.perform(get("/media/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /media/{id} — should soft-delete media")
    void deleteMedia_Success() throws Exception {
        doNothing().when(mediaService).deleteMedia(1L);

        mockMvc.perform(delete("/media/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Media soft-deleted"));
    }

    @Test
    @DisplayName("DELETE /media/post/{postId} — should soft-delete media by post")
    void deleteMediaByPost_Success() throws Exception {
        doNothing().when(mediaService).softDeleteByPost(10L);

        mockMvc.perform(delete("/media/post/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Media for post soft-deleted"));
    }

    @Test
    @DisplayName("POST /media/stories — should create story from URL")
    void createStory_Success() throws Exception {
        when(mediaService.createStory("user@gmail.com", "https://example.com/story.jpg", "My story"))
                .thenReturn(testStory);

        mockMvc.perform(post("/media/stories")
                        .header("X-User-Email", "user@gmail.com")
                        .param("mediaUrl", "https://example.com/story.jpg")
                        .param("caption", "My story"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /media/stories/active — should return active stories")
    void getActiveStories_Success() throws Exception {
        when(mediaService.getActiveStories()).thenReturn(List.of(testStory));

        mockMvc.perform(get("/media/stories/active"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /media/stories/user/{userEmail} — should return user stories")
    void getUserStories_Success() throws Exception {
        when(mediaService.getUserStories("user@gmail.com")).thenReturn(List.of(testStory));

        mockMvc.perform(get("/media/stories/user/user@gmail.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /media/stories/{id} — should return story by id")
    void getStoryById_Success() throws Exception {
        when(mediaService.getStoryById(1L)).thenReturn(testStory);

        mockMvc.perform(get("/media/stories/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /media/stories/{id}/view — should record story view")
    void viewStory_Success() throws Exception {
        doNothing().when(mediaService).viewStory(1L, "viewer@gmail.com");

        mockMvc.perform(post("/media/stories/1/view")
                        .header("X-User-Email", "viewer@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Story view recorded"));
    }

    @Test
    @DisplayName("DELETE /media/stories/{id} — should delete story")
    void deleteStory_Success() throws Exception {
        doNothing().when(mediaService).deleteStory(1L, "user@gmail.com");

        mockMvc.perform(delete("/media/stories/1")
                        .header("X-User-Email", "user@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Story deleted"));
    }

    @Test
    @DisplayName("POST /media/stories/{id}/comment — should add comment to story")
    void commentOnStory_Success() throws Exception {
        doNothing().when(mediaService).commentOnStory(1L, "user@gmail.com", "Nice story!");

        mockMvc.perform(post("/media/stories/1/comment")
                        .header("X-User-Email", "user@gmail.com")
                        .param("content", "Nice story!"))
                .andExpect(status().isOk())
                .andExpect(content().string("Comment sent"));
    }

    @Test
    @DisplayName("GET /media/test — should return health check")
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/media/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Media Service is running"));
    }
}
