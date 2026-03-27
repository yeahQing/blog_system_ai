package com.blog.api.service;

import com.blog.api.entity.Article;
import com.blog.api.entity.Tag;
import com.blog.api.repository.ArticleRepository;
import com.blog.api.repository.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;

    private void mergeTags(Article target, Set<Tag> tags) {
        if (tags == null) {
            return;
        }
        Set<Tag> next = new HashSet<>();
        for (Tag t : tags) {
            if (t != null && t.getId() != null) {
                tagRepository.findById(t.getId()).ifPresent(next::add);
            }
        }
        target.setTags(next);
    }

    @Override
    @Transactional
    public Article saveArticle(Article incoming) {
        if (incoming.getTitle() == null || incoming.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (incoming.getId() == null) {
            if (incoming.getViews() == null) {
                incoming.setViews(0);
            }
            if (incoming.getLikeCount() == null) {
                incoming.setLikeCount(0);
            }
            if (incoming.getIsDeleted() == null) {
                incoming.setIsDeleted(false);
            }
            mergeTags(incoming, incoming.getTags());
            log.info("新建文章: {}", incoming.getTitle());
            return articleRepository.save(incoming);
        }
        Article existing = articleRepository.findById(incoming.getId())
                .orElseThrow(() -> new EntityNotFoundException("文章不存在: " + incoming.getId()));
        existing.setTitle(incoming.getTitle());
        existing.setContent(incoming.getContent());
        existing.setHtmlContent(incoming.getHtmlContent());
        existing.setSummary(incoming.getSummary());
        existing.setCover(incoming.getCover());
        if (incoming.getIsDraft() != null) {
            existing.setIsDraft(incoming.getIsDraft());
        }
        existing.setCategory(incoming.getCategory());
        if (incoming.getTags() != null) {
            mergeTags(existing, incoming.getTags());
        }
        log.info("更新文章 id={}: {}", existing.getId(), existing.getTitle());
        return articleRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> getArticleForAdminById(Long id) {
        return articleRepository.findById(id);
    }

    @Override
    @Transactional
    public Optional<Article> getArticleForPortal(Long id) {
        return articleRepository.findByIdAndIsDraftFalseAndIsDeletedFalse(id).map(article -> {
            article.setViews(article.getViews() + 1);
            return articleRepository.save(article);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Article> getPortalArticles(Long categoryId, Long tagId, String keyword, Pageable pageable) {
        String kw = keyword == null ? "" : keyword.trim();
        if (categoryId == null && tagId == null && kw.isEmpty()) {
            return articleRepository.findByIsDraftAndIsDeleted(false, false, pageable);
        }
        return articleRepository.searchPortal(categoryId, tagId, kw, pageable);
    }

    @Override
    public Page<Article> getArticlesForAdmin(String keyword, Pageable pageable) {
        log.info("分页查询文章 (后台管理), 关键字: {}", keyword);
        if (keyword != null && !keyword.isBlank()) {
            return articleRepository.findByTitleContainingIgnoreCaseAndIsDeletedFalse(keyword.trim(), pageable);
        }
        return articleRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        log.warn("逻辑删除文章, ID: {}", id);
        articleRepository.findById(id).ifPresent(article -> {
            article.setIsDeleted(true);
            articleRepository.save(article);
        });
    }

    @Override
    public void incrementViews(Long id) {
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> getRelatedArticles(Long articleId, int limit) {
        int n = Math.min(Math.max(limit, 1), 10);
        Pageable p = PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return articleRepository.findById(articleId).map(a -> {
            if (a.getCategory() != null && a.getCategory().getId() != null) {
                Page<Article> byCat = articleRepository.findByCategory_IdAndIsDraftFalseAndIsDeletedFalseAndIdNot(
                        a.getCategory().getId(), articleId, p);
                if (byCat.hasContent()) {
                    return byCat.getContent();
                }
            }
            return articleRepository.findByIsDraftFalseAndIsDeletedFalseAndIdNot(articleId, p).getContent();
        }).orElse(List.of());
    }

    @Override
    @Transactional
    public boolean likeArticle(Long id) {
        return articleRepository.incrementLikeCount(id) > 0;
    }

    @Override
    @Transactional
    public Article publishArticle(Long id) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在: " + id));
        if (Boolean.TRUE.equals(a.getIsDeleted())) {
            throw new IllegalArgumentException("已删除的文章无法发布");
        }
        a.setIsDraft(false);
        log.info("文章上架(发布) id={}", id);
        return articleRepository.save(a);
    }

    @Override
    @Transactional
    public Article unpublishArticle(Long id) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在: " + id));
        if (Boolean.TRUE.equals(a.getIsDeleted())) {
            throw new IllegalArgumentException("已删除的文章无法下架");
        }
        a.setIsDraft(true);
        log.info("文章下架(改草稿) id={}", id);
        return articleRepository.save(a);
    }
}
