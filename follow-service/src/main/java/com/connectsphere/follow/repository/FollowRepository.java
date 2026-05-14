package com.connectsphere.follow.repository;
import com.connectsphere.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// JPA repository for follow graph queries.
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    // Single follow edge lookup.
    Optional<Follow> findByFollowerEmailAndFollowingEmail(String follower, String following);
    // Fast existence check used by follow/unfollow guards.
    boolean existsByFollowerEmailAndFollowingEmail(String follower, String following);
    // Followers of given user.
    List<Follow> findByFollowingEmail(String followingEmail);
    // People that given user follows.
    List<Follow> findByFollowerEmail(String followerEmail);
    // Total followers of given user.
    long countByFollowingEmail(String followingEmail);
    // Total following count of given user.
    long countByFollowerEmail(String followerEmail);
}
