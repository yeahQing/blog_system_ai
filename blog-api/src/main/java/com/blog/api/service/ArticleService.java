package com.blog.api.service;

import com.blog.api.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 文章业务接口
 */
public interface ArticleService {

    Article saveArticle(Article article);

    Optional<Article> getArticleForAdminById(Long id);

    Optional<Article> getArticleForPortal(Long id);

    /**
     * 前台列表：分类、标签、关键词（标题+正文 LIKE）
     */
    Page<Article> getPortalArticles(Long categoryId, Long tagId, String keyword, Pageable pageable);

    Page<Article> getArticlesForAdmin(String keyword, Pageable pageable);

    void deleteArticle(Long id);

    void incrementViews(Long id);

    List<Article> getRelatedArticles(Long articleId, int limit);

    boolean likeArticle(Long id);

    /**
     * 管理端：将文章设为已发布（前台可见）
     */
    Article publishArticle(Long id);

    /**
     * 管理端：下架为草稿（前台不可见）
     */
    Article unpublishArticle(Long id);
}
