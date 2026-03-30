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
     * 管理端回填标签：直接读中间表，避免仅依赖 LAZY/JPQL FETCH 时 tags 仍为空。
     */
    @Query(value = "SELECT tag_id FROM blog_article_tag WHERE article_id = :articleId", nativeQuery = true)
    List<Long> findTagIdsByArticleId(@Param("articleId") Long articleId);

    /**
     * 前台默认列表：已发布且未删除；置顶优先，再按更新时间、创建时间倒序。
     */
    Page<Article> findByIsDraftAndIsDeletedOrderByPinnedDescUpdatedAtDescCreatedAtDesc(
            Boolean isDraft, Boolean isDeleted, Pageable pageable);

    /**
     * 标题全文模糊查询 (可用于前台搜索)
     */
    Page<Article> findByTitleContainingAndIsDraftAndIsDeleted(String title, Boolean isDraft, Boolean isDeleted, Pageable pageable);

    /**
     * 管理后台列表：未删除；可选草稿/已发布；可选标题模糊；按更新时间、创建时间倒序。
     */
    @Query("""
            SELECT a FROM Article a
            WHERE a.isDeleted = false
            AND (:draft IS NULL OR a.isDraft = :draft)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY a.pinned DESC, a.updatedAt DESC, a.createdAt DESC
            """)
    Page<Article> findAdminList(
            @Param("draft") Boolean draft,
            @Param("keyword") String keyword,
            Pageable pageable);

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
            ORDER BY a.pinned DESC, a.updatedAt DESC, a.createdAt DESC
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

    /** 删除分类前：解除所有文章对该分类的外键引用 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Article a SET a.category = null WHERE a.category.id = :categoryId")
    int clearCategoryByCategoryId(@Param("categoryId") Long categoryId);

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndIsDraft(boolean isDraft);

    @Query("SELECT COALESCE(SUM(a.views), 0) FROM Article a WHERE a.isDeleted = false")
    long sumViewsNotDeleted();
}
