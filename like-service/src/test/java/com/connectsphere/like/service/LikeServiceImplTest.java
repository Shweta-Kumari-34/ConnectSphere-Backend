package com.connectsphere.like.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.like.entity.LikeEntity;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.producer.NotificationEventProducer;
import com.connectsphere.like.exception.ConflictException;
import com.connectsphere.like.exception.ResourceNotFoundException;
import com.connectsphere.like.service.impl.LikeServiceImpl;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private LikeServiceImpl likeService;

    private LikeEntity testLike;

    @BeforeEach
    void setUp() {
        testLike = new LikeEntity();
        testLike.setId(1L);
        testLike.setTargetId(10L);
        testLike.setTargetType("POST");
        testLike.setUserEmail("user@gmail.com");
        testLike.setReactionType("LIKE");
    }

    @Test
    @DisplayName("LikeTarget - Story: should update existing reaction instead of throwing conflict")
    void likeTarget_Story_UpdateExisting() {
        testLike.setTargetType("STORY");
        
        // Mock recipient resolution (Story Service)
        Map<String, Object> storyOwner = new HashMap<>();
        storyOwner.put("userEmail", "other@gmail.com");
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L))).thenReturn(storyOwner);
        
        when(likeRepository.findByTargetIdAndTargetTypeAndUserEmail(10L, "STORY", "user@gmail.com"))
                .thenReturn(Optional.of(testLike));
        when(likeRepository.save(any(LikeEntity.class))).thenReturn(testLike);

        LikeEntity result = likeService.likeTarget("user@gmail.com", 10L, "STORY", "LOVE");

        assertEquals("LOVE", result.getReactionType());
        verify(likeRepository).save(testLike);
        // Should trigger notification
        verify(notificationEventProducer, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("GetLikesByTarget - should return list")
    void getLikesByTarget_Success() {
        when(likeRepository.findByTargetIdAndTargetType(10L, "POST")).thenReturn(List.of(testLike));
        List<LikeEntity> result = likeService.getLikesByTarget(10L, "POST");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetLikesByUser - should return list")
    void getLikesByUser_Success() {
        when(likeRepository.findByUserEmail("user@gmail.com")).thenReturn(List.of(testLike));
        List<LikeEntity> result = likeService.getLikesByUser("user@gmail.com");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetLikeCountByType - should return count")
    void getLikeCountByType_Success() {
        when(likeRepository.countByTargetIdAndTargetTypeAndReactionType(10L, "POST", "LIKE")).thenReturn(5L);
        assertEquals(5L, likeService.getLikeCountByType(10L, "POST", "LIKE"));
    }

    @Test
    @DisplayName("GetReactionSummary - should return map")
    void getReactionSummary_Success() {
        LikeEntity love = new LikeEntity();
        love.setReactionType("LOVE");
        when(likeRepository.findByTargetIdAndTargetType(10L, "POST")).thenReturn(List.of(testLike, love));

        Map<String, Long> result = likeService.getReactionSummary(10L, "POST");

        assertEquals(1L, result.get("LIKE"));
        assertEquals(1L, result.get("LOVE"));
    }

    @Test
    @DisplayName("Notification Dispatch - Post Like: should fetch recipient and publish event")
    void sendLikeNotification_Post_Success() {
        // Mock recipient resolution (Post Service)
        Map<String, Object> postOwner = new HashMap<>();
        postOwner.put("userEmail", "owner@gmail.com");
        postOwner.put("imageUrl", "http://thumb.jpg");
        postOwner.put("caption", "Hello world");
        
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L))).thenReturn(postOwner);
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com")).thenReturn(false);
        when(likeRepository.save(any())).thenReturn(testLike);

        likeService.likeTarget("user@gmail.com", 10L, "POST", "LIKE");

        verify(notificationEventProducer).publish(any());
    }

    @Test
    @DisplayName("Notification Dispatch - Story Like: should fetch recipient and publish event")
    void sendLikeNotification_Story_Success() {
        testLike.setTargetType("STORY");
        Map<String, Object> storyOwner = new HashMap<>();
        storyOwner.put("userEmail", "owner@gmail.com");
        storyOwner.put("mediaUrl", "http://story.jpg");
        
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L))).thenReturn(storyOwner);
        when(likeRepository.findByTargetIdAndTargetTypeAndUserEmail(10L, "STORY", "user@gmail.com")).thenReturn(Optional.empty());
        when(likeRepository.save(any())).thenReturn(testLike);

        likeService.likeTarget("user@gmail.com", 10L, "STORY", "LIKE");

        verify(notificationEventProducer).publish(any());
    }
    @Test
    @DisplayName("LikeTarget - should save and return like")
    void likeTarget_Success() {
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com")).thenReturn(false);
        when(likeRepository.save(any(LikeEntity.class))).thenReturn(testLike);

        LikeEntity result = likeService.likeTarget("user@gmail.com", 10L, "POST", "LIKE");

        assertNotNull(result);
        assertEquals("LIKE", result.getReactionType());
        verify(likeRepository).save(any(LikeEntity.class));
    }

    @Test
    @DisplayName("LikeTarget - should throw when already liked")
    void likeTarget_AlreadyLiked() {
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> likeService.likeTarget("user@gmail.com", 10L, "POST", "LIKE"));
        verify(likeRepository, never()).save(any());
    }

    @Test
    @DisplayName("UnlikeTarget - should delete like")
    void unlikeTarget_Success() {
        when(likeRepository.findByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com"))
                .thenReturn(Optional.of(testLike));

        likeService.unlikeTarget("user@gmail.com", 10L, "POST");

        verify(likeRepository).delete(testLike);
    }

    @Test
    @DisplayName("UnlikeTarget - should throw when not found")
    void unlikeTarget_NotFound() {
        when(likeRepository.findByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> likeService.unlikeTarget("user@gmail.com", 10L, "POST"));
    }

    @Test
    @DisplayName("HasLiked - should return true when liked")
    void hasLiked_True() {
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com")).thenReturn(true);

        assertTrue(likeService.hasLiked("user@gmail.com", 10L, "POST"));
    }

    @Test
    @DisplayName("GetLikeCount - should return count")
    void getLikeCount_Success() {
        when(likeRepository.countByTargetIdAndTargetType(10L, "POST")).thenReturn(42L);

        assertEquals(42L, likeService.getLikeCount(10L, "POST"));
    }

    @Test
    @DisplayName("ChangeReaction - should update reaction type")
    void changeReaction_Success() {
        when(likeRepository.findByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com"))
                .thenReturn(Optional.of(testLike));
        when(likeRepository.save(any(LikeEntity.class))).thenReturn(testLike);

        LikeEntity result = likeService.changeReaction("user@gmail.com", 10L, "POST", "LOVE");

        verify(likeRepository).save(any(LikeEntity.class));
    }
    @Test
    @DisplayName("Notification Dispatch - Reel Like: should fetch recipient and publish event")
    void sendLikeNotification_Reel_Success() {
        testLike.setTargetType("REEL");
        Map<String, Object> reelOwner = new HashMap<>();
        reelOwner.put("userEmail", "reel_owner@gmail.com");
        reelOwner.put("mediaUrl", "http://reel.mp4");
        
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L))).thenReturn(reelOwner);
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "REEL", "user@gmail.com")).thenReturn(false);
        when(likeRepository.save(any())).thenReturn(testLike);

        likeService.likeTarget("user@gmail.com", 10L, "REEL", "LIKE");

        verify(notificationEventProducer).publish(any());
    }

    @Test
    @DisplayName("Notification Dispatch - Comment Like: should fetch recipient and resolve parent postId")
    void sendLikeNotification_Comment_Success() {
        testLike.setTargetType("COMMENT");
        
        // Mock comment recipient resolution
        Map<String, Object> comment = new HashMap<>();
        comment.put("userEmail", "commenter@gmail.com");
        comment.put("postId", 500L); // Parent Post ID
        
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L))).thenReturn(comment);
        
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "COMMENT", "user@gmail.com")).thenReturn(false);
        when(likeRepository.save(any())).thenReturn(testLike);

        likeService.likeTarget("user@gmail.com", 10L, "COMMENT", "LIKE");

        verify(notificationEventProducer).publish(any());
    }

    @Test
    @DisplayName("Notification Dispatch - Error Handling: should not crash if recipient lookup fails")
    void sendLikeNotification_RecipientNotFound_Safe() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq(10L))).thenReturn(null);
        when(likeRepository.existsByTargetIdAndTargetTypeAndUserEmail(10L, "POST", "user@gmail.com")).thenReturn(false);
        when(likeRepository.save(any())).thenReturn(testLike);

        assertDoesNotThrow(() -> likeService.likeTarget("user@gmail.com", 10L, "POST", "LIKE"));
        
        verify(notificationEventProducer, never()).publish(any());
    }
}
