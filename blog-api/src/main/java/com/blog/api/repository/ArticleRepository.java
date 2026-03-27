package com.blog.api.repository;

import com.blog.api.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文章持久层
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * 根据是否草稿分页查询文章
     */
    Page<Article> findByIsDraftAndIsDeleted(Boolean isDraft, Boolean isDeleted, Pageable pageable);

    /**
     * 标题全文模糊查询 (可用于前台搜索)
     */
    Page<Article> findByTitleContainingAndIsDraftAndIsDeleted(String title, Boolean isDraft, Boolean isDeleted, Pageable pageable);

    /**
     * 管理后台列表：未逻辑删除
     */
    Page<Article> findByIsDeletedFalse(Pageable pageable);

    /**
     * 管理后台标题搜索（含草稿），排除已逻辑删除
     */
    Page<Article> findByTitleContainingIgnoreCaseAndIsDeletedFalse(String title, Pageable pageable);

    /**
     * 前台公开详情：已发布且未删除
     */
    Optional<Article> findByIdAndIsDraftFalseAndIsDeletedFalse(Long id);

    @Query("""
            SELECT DISTINCT a FROM Article a
            LEFT JOIN a.tags tg
            WHERE a.isDraft = false AND a.isDeleted = false
            AND (:categoryId IS NULL OR a.category.id = :categoryId)
            AND (:tagId IS NULL OR tg.id = :tagId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Article> searchPortal(
            @Param("categoryId") Long categoryId,
            @Param("tagId") Long tagId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Page<Article> findByCategory_IdAndIsDraftFalseAndIsDeletedFalseAndIdNot(
            Long categoryId, Long idNot, Pageable pageable);

    Page<Article> findByIsDraftFalseAndIsDeletedFalseAndIdNot(Long idNot, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Article a SET a.likeCount = COALESCE(a.likeCount, 0) + 1 WHERE a.id = :id AND a.isDeleted = false AND a.isDraft = false")
    int incrementLikeCount(@Param("id") Long id);

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndIsDraft(boolean isDraft);

    @Query("SELECT COALESCE(SUM(a.views), 0) FROM Article a WHERE a.isDeleted = false")
    long sumViewsNotDeleted();
}
