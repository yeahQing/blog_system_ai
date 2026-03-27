package com.blog.api.repository;

import com.blog.api.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByArticleIdAndApprovedTrueOrderByCreatedAtAsc(Long articleId);

    long countByParentId(Long parentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = COALESCE(c.likeCount, 0) + 1 WHERE c.id = :id")
    int incrementLike(@Param("id") Long id);
}
