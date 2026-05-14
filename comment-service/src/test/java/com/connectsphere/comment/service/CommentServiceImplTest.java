package com.connectsphere.comment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.connectsphere.comment.dto.NotificationEvent;
import com.connectsphere.comment.util.CommentConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.comment.dto.CommentRequestDto;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.producer.NotificationEventProducer;
import com.connectsphere.comment.exception.ResourceNotFoundException;
import com.connectsphere.comment.exception.UnauthorizedException;
import com.connectsphere.comment.service.impl.CommentServiceImpl;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment testComment;
    private CommentRequestDto requestDto;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setPostId(10L);
        testComment.setUserEmail("user@gmail.com");
        testComment.setContent("Great post!");
        testComment.setLikeCount(0);

        requestDto = new CommentRequestDto();
        requestDto.setContent("Great post!");
    }

    @Test
    @DisplayName("AddComment - should save and return comment")
    void addComment_Success() {
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        Comment result = commentService.addComment("user@gmail.com", requestDto);

        assertNotNull(result);
        assertEquals("Great post!", result.getContent());
        assertEquals("user@gmail.com", result.getUserEmail());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("GetCommentsByPost - should return all visible comments for a post")
    void getCommentsByPost_Success() {
        when(commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(10L)).thenReturn(List.of(testComment));

        List<Comment> result = commentService.getCommentsByPost(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getPostId());
    }

    @Test
    @DisplayName("GetCommentById - should return when found")
    void getCommentById_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        Comment result = commentService.getCommentById(1L);

        assertNotNull(result);
        assertEquals("Great post!", result.getContent());
    }

    @Test
    @DisplayName("GetCommentById - should throw when not found")
    void getCommentById_NotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentById(99L));
    }

    @Test
    @DisplayName("UpdateComment - should throw when not owner")
    void updateComment_Unauthorized() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertThrows(UnauthorizedException.class,
                () -> commentService.updateComment(1L, "hacker@gmail.com", "hacked!"));
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("DeleteComment - should throw when not owner")
    void deleteComment_Unauthorized() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertThrows(UnauthorizedException.class,
                () -> commentService.deleteComment(1L, "hacker@gmail.com"));
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("GetCommentsByUser - should return user comments")
    void getCommentsByUser_Success() {
        when(commentRepository.findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc("user@gmail.com"))
                .thenReturn(List.of(testComment));

        List<Comment> result = commentService.getCommentsByUser("user@gmail.com");

        assertEquals(1, result.size());
        assertEquals("user@gmail.com", result.get(0).getUserEmail());
    }

    @Test
    @DisplayName("LikeComment - should increment like count")
    void likeComment_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        commentService.likeComment(1L);

        assertEquals(1, testComment.getLikeCount());
        verify(commentRepository).save(testComment);
    }

    @Test
    @DisplayName("UnlikeComment - should decrement like count but not below 0")
    void unlikeComment_Success() {
        testComment.setLikeCount(1);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        commentService.unlikeComment(1L);
        assertEquals(0, testComment.getLikeCount());

        commentService.unlikeComment(1L);
        assertEquals(0, testComment.getLikeCount());
    }

    @Test
    @DisplayName("AddComment - should send notification for post comment")
    void addComment_Notification_Post() {
        requestDto.setPostId(100L);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        
        Map<String, Object> mockPost = new HashMap<>();
        mockPost.put("userEmail", "author@test.com");
        mockPost.put("mediaUrl", "http://media.com/1.jpg");
        
        when(restTemplate.getForObject(eq(CommentConstants.POST_SERVICE_URL), eq(Map.class), (Object) any()))
                .thenReturn(mockPost);

        commentService.addComment("user@gmail.com", requestDto);

        verify(notificationEventProducer).publish(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("AddComment - should send notification for reply")
    void addComment_Notification_Reply() {
        requestDto.setPostId(100L);
        requestDto.setParentId(1L);
        
        Comment parent = new Comment();
        parent.setId(1L);
        parent.setUserEmail("parent@test.com");
        
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parent));

        commentService.addComment("user@gmail.com", requestDto);

        verify(commentRepository).findById(1L); // Ensure it actually fetched the parent
        verify(notificationEventProducer).publish(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("AddComment - should NOT send notification for self-comment")
    void addComment_NoNotification_Self() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        
        Map<String, Object> mockPost = new HashMap<>();
        mockPost.put("userEmail", "user@gmail.com");
        
        when(restTemplate.getForObject(eq(CommentConstants.POST_SERVICE_URL), eq(Map.class), (Object) any()))
                .thenReturn(mockPost);

        commentService.addComment("user@gmail.com", requestDto);

        verify(notificationEventProducer, never()).publish(any());
    }

    @Test
    @DisplayName("AddComment - should send notification for reel comment (fallback)")
    void addComment_Notification_Reel() {
        requestDto.setPostId(200L);
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        
        // Post service fails
        when(restTemplate.getForObject(eq(CommentConstants.POST_SERVICE_URL), eq(Map.class), (Object) any()))
                .thenThrow(new RuntimeException("Post not found"));
        
        // Reel service succeeds
        Map<String, Object> mockReel = new HashMap<>();
        mockReel.put("userEmail", "reel_author@test.com");
        when(restTemplate.getForObject(eq(CommentConstants.REEL_SERVICE_URL), eq(Map.class), (Object) any()))
                .thenReturn(mockReel);

        commentService.addComment("user@gmail.com", requestDto);

        verify(notificationEventProducer).publish(argThat(event -> 
            event.getRecipientEmail().equals("reel_author@test.com") &&
            event.getMessage().contains("commented on your reel")
        ));
    }
}
