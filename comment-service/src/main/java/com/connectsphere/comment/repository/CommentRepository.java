package com.connectsphere.comment.repository;

import com.connectsphere.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// JPA repository for post comments and threaded replies.
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);
    List<Comment> findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc(String userEmail);
    long countByPostIdAndIsDeletedFalse(Long postId);
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
    void deleteById(Long id);
}
