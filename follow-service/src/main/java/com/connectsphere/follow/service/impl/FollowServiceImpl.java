package com.connectsphere.follow.service.impl;

import com.connectsphere.follow.dto.NotificationEvent;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.producer.NotificationEventProducer;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.FollowService;
import com.connectsphere.follow.util.FollowConstants;
import com.connectsphere.follow.exception.ResourceNotFoundException;
import com.connectsphere.follow.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h1>FollowServiceImpl</h1>
 * <p>High-performance implementation of {@link FollowService} that manages complex social relationships 
 * and second-degree connection discovery.</p>
 * 
 * <h2>Social Discovery Workflow (People You May Know):</h2>
 * <pre>
 * graph TD
 *     Start[User A] --> F1[Fetch Users Followed by A]
 *     F1 --> F2[Fetch Users Followed by F1 Members]
 *     F2 --> Filter{Exclude Self & Already Followed}
 *     Filter --> Result[Second-Degree Suggestions]
 * </pre>
 * 
 * <h2>Key Logic Features:</h2>
 * <ul>
 *     <li><b>Email Normalization:</b> Ensures consistent lookups across case-sensitive input variants.</li>
 *     <li><b>Event-Driven Notifications:</b> Uses {@link NotificationEventProducer} for decoupling follow alerts.</li>
 *     <li><b>Graph Safety:</b> Prevents self-follows and redundant follow mutations.</li>
 *     <li><b>Set Operations:</b> Optimizes mutual follow and suggestion logic using Java Streams and Set intersections.</li>
 * </ul>
 */
@Service
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final FollowRepository followRepository;
    private final NotificationEventProducer notificationEventProducer;

    public FollowServiceImpl(FollowRepository followRepository, NotificationEventProducer notificationEventProducer) {
        this.followRepository = followRepository;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Override
    public Follow follow(String followerEmail, String followingEmail) {
        // Normalize emails to keep matching/caching consistent across requests.
        followerEmail = normalizeEmail(followerEmail);
        followingEmail = normalizeEmail(followingEmail);
        // Protect invalid follow graph mutations.
        if (followerEmail.equals(followingEmail)) throw new ConflictException("Cannot follow yourself");
        if (followRepository.existsByFollowerEmailAndFollowingEmail(followerEmail, followingEmail))
            throw new ConflictException("Already following");
        Follow follow = new Follow();
        follow.setFollowerEmail(followerEmail);
        follow.setFollowingEmail(followingEmail);
        follow.setStatus(FollowConstants.STATUS_ACTIVE);
        follow.setCreatedAt(LocalDateTime.now());
        Follow saved = followRepository.save(follow);
        // Follow action triggers asynchronous notification to target user.
        sendFollowNotification(saved);
        return saved;
    }

    @Override
    public void unfollow(String followerEmail, String followingEmail) {
        followerEmail = normalizeEmail(followerEmail);
        followingEmail = normalizeEmail(followingEmail);
        // Throwing not-found keeps API response clear for missing relations.
        Follow follow = followRepository.findByFollowerEmailAndFollowingEmail(followerEmail, followingEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Not following this user"));
        followRepository.delete(follow);
    }

    @Override public boolean isFollowing(String f, String fg) { return followRepository.existsByFollowerEmailAndFollowingEmail(normalizeEmail(f), normalizeEmail(fg)); }
    @Override public List<Follow> getFollowers(String e) { return followRepository.findByFollowingEmail(normalizeEmail(e)); }
    @Override public List<Follow> getFollowing(String e) { return followRepository.findByFollowerEmail(normalizeEmail(e)); }
    @Override public long getFollowerCount(String e) { return followRepository.countByFollowingEmail(normalizeEmail(e)); }
    @Override public long getFollowingCount(String e) { return followRepository.countByFollowerEmail(normalizeEmail(e)); }

    @Override
    public List<String> getMutualFollows(String userEmail1, String userEmail2) {
        userEmail1 = normalizeEmail(userEmail1);
        userEmail2 = normalizeEmail(userEmail2);
        // Mutual = users that BOTH userEmail1 and userEmail2 follow
        Set<String> user1Following = followRepository.findByFollowerEmail(userEmail1)
                .stream().map(Follow::getFollowingEmail).collect(Collectors.toSet());
        Set<String> user2Following = followRepository.findByFollowerEmail(userEmail2)
                .stream().map(Follow::getFollowingEmail).collect(Collectors.toSet());
        user1Following.retainAll(user2Following);
        return List.copyOf(user1Following);
    }

    @Override
    public List<String> getSuggestedUsers(String userEmail) {
        final String normalizedUserEmail = normalizeEmail(userEmail);
        // Suggest second-degree connections:
        // users followed by people I follow, excluding me and already-followed users.
        Set<String> myFollowing = followRepository.findByFollowerEmail(normalizedUserEmail)
                .stream().map(Follow::getFollowingEmail).collect(Collectors.toSet());
        Set<String> suggestions = myFollowing.stream()
                .flatMap(followee -> followRepository.findByFollowerEmail(followee).stream())
                .map(Follow::getFollowingEmail)
                .filter(email -> !email.equals(normalizedUserEmail))
                .filter(email -> !myFollowing.contains(email))
                .collect(Collectors.toSet());
        return List.copyOf(suggestions);
    }

    private void sendFollowNotification(Follow follow) {
        String recipientEmail = follow.getFollowingEmail();
        String actorEmail = follow.getFollowerEmail();
        // Skip invalid/self notifications.
        if (recipientEmail == null || actorEmail == null || recipientEmail.equalsIgnoreCase(actorEmail)) {
            return;
        }
        try {
            NotificationEvent event = new NotificationEvent();
            event.setRecipientEmail(recipientEmail);
            event.setActorEmail(actorEmail);
            event.setType(FollowConstants.NOTIFICATION_TYPE_FOLLOW);
            event.setDeepLinkUrl(FollowConstants.DEEP_LINK_USER + actorEmail);
            event.setMessage(actorEmail.split("@")[0] + " started following you");
            event.setPriority(FollowConstants.NOTIFICATION_PRIORITY_HIGH);
            event.setCreatedAt(LocalDateTime.now());
            notificationEventProducer.publish(event);
        } catch (Exception ignored) {}
    }

    private String normalizeEmail(String email) {
        // Canonical format used across repository lookups.
        return email == null ? "" : email.trim().toLowerCase();
    }
}
