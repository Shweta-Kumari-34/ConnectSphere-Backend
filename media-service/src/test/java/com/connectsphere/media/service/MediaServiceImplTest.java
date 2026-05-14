package com.connectsphere.media.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import com.connectsphere.media.config.UploadConfig;
import com.connectsphere.media.producer.NotificationEventProducer;
import com.connectsphere.media.exception.ResourceNotFoundException;
import com.connectsphere.media.exception.UnauthorizedException;
import com.connectsphere.media.exception.BadRequestException;
import com.connectsphere.media.service.impl.MediaServiceImpl;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private UploadConfig uploadConfig;
    @Mock private NotificationEventProducer notificationEventProducer;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private MediaServiceImpl mediaService;

    private Media testMedia;
    private Story testStory;

    @BeforeEach
    void setUp() {
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
    @DisplayName("UploadMedia - should save and return media")
    void uploadMedia_Success() {
        when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
        Media result = mediaService.uploadMedia("user@gmail.com", 10L, "https://example.com/photo.jpg", "IMAGE");
        assertNotNull(result);
        assertEquals("IMAGE", result.getMediaType());
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("GetMediaByPost - should return media list")
    void getMediaByPost_Success() {
        when(mediaRepository.findByPostIdAndIsDeletedFalse(10L)).thenReturn(List.of(testMedia));
        assertEquals(1, mediaService.getMediaByPost(10L).size());
    }

    @Test
    @DisplayName("CreateStory - should save story")
    void createStory_Success() {
        when(storyRepository.save(any(Story.class))).thenReturn(testStory);
        Story result = mediaService.createStory("user@gmail.com", "https://example.com/story.jpg", "My story");
        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("CreateStory - should reject video URLs")
    void createStory_WithVideoUrl_ShouldFail() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mediaService.createStory("user@gmail.com", "https://example.com/story.mp4", "My story"));

        assertEquals("Stories currently support image URLs only", ex.getMessage());
    }

    @Test
    @DisplayName("ViewStory - should increment views for another user")
    void viewStory_ForAnotherUser_IncrementsCount() {
        testStory.setViewsCount(3);
        testStory.setExpiresAt(java.time.LocalDateTime.now().plusHours(1));
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        mediaService.viewStory(1L, "viewer@gmail.com");

        assertEquals(4, testStory.getViewsCount());
        verify(storyRepository).save(testStory);
    }

    @Test
    @DisplayName("ViewStory - should not increment views for owner")
    void viewStory_ForOwner_DoesNotIncrementCount() {
        testStory.setViewsCount(3);
        testStory.setExpiresAt(java.time.LocalDateTime.now().plusHours(1));
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        mediaService.viewStory(1L, "user@gmail.com");

        assertEquals(3, testStory.getViewsCount());
        verify(storyRepository, never()).save(testStory);
    }

    @Test
    @DisplayName("DeleteStory - should throw when not owner")
    void deleteStory_Unauthorized() {
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        assertThrows(UnauthorizedException.class, () -> mediaService.deleteStory(1L, "hacker@gmail.com"));
    }

    @Test
    @DisplayName("DeleteStory - should delete when owner")
    void deleteStory_Success() {
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        mediaService.deleteStory(1L, "user@gmail.com");
        verify(storyRepository).delete(testStory);
    }

    @Test
    @DisplayName("ExpireOldStories - should purge expired stories")
    void expireOldStories_Success() {
        when(storyRepository.findByActiveTrueAndExpiresAtLessThanEqual(any())).thenReturn(List.of(testStory));

        mediaService.expireOldStories();

        verify(storyRepository).deleteAll(List.of(testStory));
    }

    @Test
    @DisplayName("ViewStory - should reject expired story")
    void viewStory_ExpiredStory_ShouldFail() {
        testStory.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> mediaService.viewStory(1L, "viewer@gmail.com"));

        assertEquals("Story has expired", ex.getMessage());
    }
}
