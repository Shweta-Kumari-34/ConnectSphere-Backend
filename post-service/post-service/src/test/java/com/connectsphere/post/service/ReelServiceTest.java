package com.connectsphere.post.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.post.entity.Reel;
import com.connectsphere.post.entity.ReelComment;
import com.connectsphere.post.repository.ReelCommentRepository;
import com.connectsphere.post.repository.ReelRepository;
import com.connectsphere.post.service.impl.ReelServiceImpl;
import com.connectsphere.post.producer.NotificationEventProducer;
import com.connectsphere.post.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ReelServiceTest {

    @Mock
    private ReelRepository reelRepository;

    @Mock
    private ReelCommentRepository reelCommentRepository;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private ReelServiceImpl reelService;

    private Reel testReel;

    @BeforeEach
    void setUp() {
        testReel = new Reel();
        testReel.setId(1L);
        testReel.setUserEmail("user1@gmail.com");
        testReel.setCaption("Test Reel");
        testReel.setVideoUrl("/uploads/reels/test.mp4");
    }

    @Test
    @DisplayName("GetReelById - Success: should return reel")
    void getReelById_Success() {
        when(reelRepository.findById(1L)).thenReturn(Optional.of(testReel));
        
        Reel result = reelService.getReelById(1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("GetReelById - Fail: should throw when not found")
    void getReelById_NotFound() {
        when(reelRepository.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> reelService.getReelById(99L));
    }

    @Test
    @DisplayName("AddComment - Success: should save comment and notify")
    void addComment_Success() {
        when(reelRepository.findById(1L)).thenReturn(Optional.of(testReel));
        ReelComment comment = new ReelComment();
        comment.setId(10L);
        comment.setContent("Nice reel!");
        comment.setUserEmail("user2@gmail.com");
        when(reelCommentRepository.save(any(ReelComment.class))).thenReturn(comment);
        
        ReelComment result = reelService.addComment("user2@gmail.com", 1L, "Nice reel!");
        
        assertNotNull(result);
        verify(reelCommentRepository).save(any(ReelComment.class));
        verify(notificationEventProducer).publish(any());
    }

    @Test
    @DisplayName("GetComments - Success: should return comments for reel")
    void getComments_Success() {
        when(reelCommentRepository.findByReelIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(new ReelComment()));
        
        List<ReelComment> results = reelService.getComments(1L);
        
        assertEquals(1, results.size());
    }
}
