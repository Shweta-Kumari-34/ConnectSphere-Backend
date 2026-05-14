package com.connectsphere.follow.service;

import com.connectsphere.follow.entity.Follow;
import java.util.List;

/**
 * <h1>FollowService Interface</h1>
 * <p>Manages the social connectivity graph, enabling users to build communities and discover peers.</p>
 * 
 * <h2>Core Functions:</h2>
 * <ul>
 *     <li><b>Graph Mutations:</b> Creation and removal of directional follow relationships.</li>
 *     <li><b>Relationship Auditing:</b> Checking status and aggregating follower/following counts.</li>
 *     <li><b>Social Discovery:</b> Identifying mutual connections and suggesting potential peers.</li>
 *     <li><b>Asynchronous Alerting:</b> Integrated hooks for notifying users of new followers.</li>
 * </ul>
 * 
 * <h2>Graph Interaction Flow:</h2>
 * <pre>
 * graph LR
 *     A[Follower] -->|Action: Follow| B[Following]
 *     B -->|Result| C{Sync Update}
 *     C --> D[(Database)]
 *     C -->|Trigger| E[Async Notification]
 *     E --> F[Follower Alert]
 * </pre>
 */
public interface FollowService {

    /**
     * Establishes a directional follow relationship from follower to following.
     */
    Follow follow(String followerEmail, String followingEmail);

    /**
     * Terminates an existing follow relationship.
     */
    void unfollow(String followerEmail, String followingEmail);

    /**
     * Checks the existence of a follow relationship between two users.
     */
    boolean isFollowing(String followerEmail, String followingEmail);

    /**
     * Lists all users who follow the target account.
     */
    List<Follow> getFollowers(String userEmail);

    /**
     * Lists all accounts that the target user is currently following.
     */
    List<Follow> getFollowing(String userEmail);

    /**
     * Aggregates the total count of followers for a user.
     */
    long getFollowerCount(String userEmail);

    /**
     * Aggregates the total count of users followed by a user.
     */
    long getFollowingCount(String userEmail);

    /**
     * Finds common users followed by both parties (Mutual Friends logic).
     */
    List<String> getMutualFollows(String userEmail1, String userEmail2);

    /**
     * Implements social discovery by suggesting users from second-degree connections.
     */
    List<String> getSuggestedUsers(String userEmail);
}
