package com.connectsphere.like.repository;

import com.connectsphere.like.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * <h1>LikeRepository</h1>
 * <p>Spring Data JPA repository responsible for persisting and querying engagement records within the database.</p>
 * 
 * <h2>Database Query Flow:</h2>
 * <pre>
 * graph LR
 *     A[LikeService] --> B{LikeRepository}
 *     B -->|JPA Method Translation| C[(MySQL DB)]
 *     C -->|Polymorphic Lookup| D[targetId + targetType]
 * </pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Custom Derived Queries:</b> Uses Spring Data JPA conventions to dynamically generate SQL queries based on method names.</li>
 *     <li><b>Polymorphic Filtering:</b> All queries strictly rely on both {@code targetId} and {@code targetType} to ensure accurate retrieval.</li>
 * </ul>
 */
@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByTargetIdAndTargetTypeAndUserEmail(Long targetId, String targetType, String userEmail);
    List<LikeEntity> findByTargetIdAndTargetType(Long targetId, String targetType);
    List<LikeEntity> findByUserEmail(String userEmail);
    long countByTargetIdAndTargetType(Long targetId, String targetType);
    boolean existsByTargetIdAndTargetTypeAndUserEmail(Long targetId, String targetType, String userEmail);
    long countByTargetIdAndTargetTypeAndReactionType(Long targetId, String targetType, String reactionType);
}
