package com.connectsphere.like.repository;

import com.connectsphere.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {
    List<Like> findByTargetTypeAndTargetId(String targetType, Long targetId);
    boolean existsByUserEmailAndTargetTypeAndTargetId(String userEmail, String targetType, Long targetId);
    void deleteByUserEmailAndTargetTypeAndTargetId(String userEmail, String targetType, Long targetId);
}
