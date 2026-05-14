package com.connectsphere.follow.service;

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

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.producer.NotificationEventProducer;
import com.connectsphere.follow.exception.ConflictException;
import com.connectsphere.follow.exception.ResourceNotFoundException;
import com.connectsphere.follow.service.impl.FollowServiceImpl;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private FollowServiceImpl followService;

    private Follow testFollow;

    @BeforeEach
    void setUp() {
        testFollow = new Follow();
        testFollow.setId(1L);
        testFollow.setFollowerEmail("user1@gmail.com");
        testFollow.setFollowingEmail("user2@gmail.com");
    }

    @Test
    @DisplayName("Follow - should save and return follow")
    void follow_Success() {
        when(followRepository.existsByFollowerEmailAndFollowingEmail("user1@gmail.com", "user2@gmail.com")).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(testFollow);

        Follow result = followService.follow("user1@gmail.com", "user2@gmail.com");

        assertNotNull(result);
        assertEquals("user1@gmail.com", result.getFollowerEmail());
        assertEquals("user2@gmail.com", result.getFollowingEmail());
        verify(followRepository).save(any(Follow.class));
    }

    @Test
    @DisplayName("Follow - should throw when following self")
    void follow_Self() {
        assertThrows(ConflictException.class,
                () -> followService.follow("user1@gmail.com", "user1@gmail.com"));
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("Follow - should throw when already following")
    void follow_AlreadyFollowing() {
        when(followRepository.existsByFollowerEmailAndFollowingEmail("user1@gmail.com", "user2@gmail.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> followService.follow("user1@gmail.com", "user2@gmail.com"));
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unfollow - should delete follow record")
    void unfollow_Success() {
        when(followRepository.findByFollowerEmailAndFollowingEmail("user1@gmail.com", "user2@gmail.com"))
                .thenReturn(Optional.of(testFollow));

        followService.unfollow("user1@gmail.com", "user2@gmail.com");

        verify(followRepository).delete(testFollow);
    }

    @Test
    @DisplayName("Unfollow - should throw when not following")
    void unfollow_NotFollowing() {
        when(followRepository.findByFollowerEmailAndFollowingEmail("user1@gmail.com", "user2@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> followService.unfollow("user1@gmail.com", "user2@gmail.com"));
    }

    @Test
    @DisplayName("IsFollowing - should return true when following")
    void isFollowing_True() {
        when(followRepository.existsByFollowerEmailAndFollowingEmail("user1@gmail.com", "user2@gmail.com")).thenReturn(true);

        assertTrue(followService.isFollowing("user1@gmail.com", "user2@gmail.com"));
    }

    @Test
    @DisplayName("GetFollowers - should return followers list")
    void getFollowers_Success() {
        when(followRepository.findByFollowingEmail("user2@gmail.com")).thenReturn(List.of(testFollow));

        List<Follow> result = followService.getFollowers("user2@gmail.com");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetFollowerCount - should return count")
    void getFollowerCount_Success() {
        when(followRepository.countByFollowingEmail("user2@gmail.com")).thenReturn(10L);

        assertEquals(10L, followService.getFollowerCount("user2@gmail.com"));
    }

    @Test
    @DisplayName("GetFollowingCount - should return count")
    void getFollowingCount_Success() {
        when(followRepository.countByFollowerEmail("user1@gmail.com")).thenReturn(5L);

        assertEquals(5L, followService.getFollowingCount("user1@gmail.com"));
    }
}
