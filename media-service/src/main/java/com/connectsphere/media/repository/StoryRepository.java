package com.connectsphere.media.repository;
import com.connectsphere.media.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByUserEmailAndActiveTrueAndExpiresAtAfter(String userEmail, LocalDateTime now);
    List<Story> findByActiveTrueAndExpiresAtAfter(LocalDateTime now);
    List<Story> findByActiveTrueAndExpiresAtLessThanEqual(LocalDateTime now);
}
