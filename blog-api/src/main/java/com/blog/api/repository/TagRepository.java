package com.blog.api.repository;

import com.blog.api.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    @Modifying
    @Query(value = "DELETE FROM blog_article_tag WHERE tag_id = :tagId", nativeQuery = true)
    void unlinkTagFromAllArticles(@Param("tagId") Long tagId);
}
