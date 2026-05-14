package com.connectsphere.media.repository;
import com.connectsphere.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByPostId(Long postId);
    List<Media> findByPostIdAndIsDeletedFalse(Long postId);
    List<Media> findByUserEmail(String userEmail);
}
