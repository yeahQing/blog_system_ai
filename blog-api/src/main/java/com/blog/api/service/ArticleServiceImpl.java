package com.blog.api.service;

import com.blog.api.dto.ArticleSaveRequest;
import com.blog.api.entity.Article;
import com.blog.api.entity.Category;
import com.blog.api.platform.rag.ArticleChunkService;
import com.blog.api.repository.ArticleRepository;
import com.blog.api.repository.CategoryRepository;
import com.blog.api.repository.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    /** @Lazy 避免与 ArticleChunkService 潜在的循环依赖；@Async 方法在事务提交后触发 */
    @Autowired
    @Lazy
    private ArticleChunkService articleChunkService;

    public ArticleServiceImpl(
            ArticleRepository articleRepository,
            TagRepository tagRepository,
            CategoryRepository categoryRepository) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.categoryRepository = categoryRepository;
    }

    private void resolveCategory(Article target, Long categoryId) {
        if (categoryId == null) {
            target.setCategory(null);
            return;
        }
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));
        target.setCategory(cat);
    }

    /** 用 id 列表关联标签，避免 JSON 反序列化到 {@code Set<Tag>} 时绑定失败 */
    private void mergeTagsByIds(Article target, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        Set<Long> ids = tagIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        target.getTags().clear();
        for (Long id : ids) {
            tagRepository.findById(id).ifPresent(t -> target.getTags().add(t));
        }
    }

    /**
     * 从中间表组装 tags，供 JSON 序列化（懒加载集合在列表/详情中否则常为空）。
     * 必须在 Hibernate 管理的集合上 clear/addAll，禁止 setTags(新 HashSet) 替换 PersistentSet，否则会破坏关联跟踪，
     * 在详情接口里再 save 文章时可能刷写中间表异常（线上表现为接口失败）。
     */
    private void attachTagsFromJoinTable(Article article) {
        if (article.getId() == null) {
            return;
        }
        List<Long> tagIds = articleRepository.findTagIdsByArticleId(article.getId());
        article.getTags().clear();
        for (Long tid : tagIds) {
            if (tid != null) {
                tagRepository.findById(tid).ifPresent(t -> article.getTags().add(t));
            }
        }
    }

    @Override
    @Transactional
    public Article saveArticle(ArticleSaveRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (req.getId() == null) {
            Article incoming = new Article();
            incoming.setTitle(req.getTitle());
            incoming.setContent(req.getContent());
            incoming.setHtmlContent(req.getHtmlContent());
            incoming.setSummary(req.getSummary());
            incoming.setCover(req.getCover());
            incoming.setIsDraft(req.getIsDraft() != null ? req.getIsDraft() : true);
            incoming.setViews(0);
            incoming.setLikeCount(0);
            incoming.setIsDeleted(req.getIsDeleted() != null ? req.getIsDeleted() : false);
            incoming.setPinned(req.getPinned() != null ? req.getPinned() : false);
            resolveCategory(incoming, req.getCategoryId());
            mergeTagsByIds(incoming, req.getTagIds() != null ? req.getTagIds() : List.of());
            log.info("新建文章: {}", incoming.getTitle());
            Article saved = articleRepository.save(incoming);
            triggerChunkUpsert(saved);
            return saved;
        }
        Article existing = articleRepository.findById(req.getId())
                .orElseThrow(() -> new EntityNotFoundException("文章不存在: " + req.getId()));
        existing.setTitle(req.getTitle());
        existing.setContent(req.getContent());
        existing.setHtmlContent(req.getHtmlContent());
        existing.setSummary(req.getSummary());
        existing.setCover(req.getCover());
        if (req.getIsDraft() != null) {
            existing.setIsDraft(req.getIsDraft());
        }
        if (req.getPinned() != null) {
            existing.setPinned(req.getPinned());
        }
        resolveCategory(existing, req.getCategoryId());
        if (req.getTagIds() != null) {
            mergeTagsByIds(existing, req.getTagIds());
        }
        log.info("更新文章 id={}: {}", existing.getId(), existing.getTitle());
        Article saved = articleRepository.save(existing);
        triggerChunkUpsert(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> getArticleForAdminById(Long id) {
        Optional<Article> opt = articleRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Article article = opt.get();
        attachTagsFromJoinTable(article);
        return Optional.of(article);
    }

    @Override
    @Transactional
    public Optional<Article> getArticleForPortal(Long id) {
        Optional<Article> opt = articleRepository.findByIdAndIsDraftFalseAndIsDeletedFalse(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Article article = opt.get();
        attachTagsFromJoinTable(article);
        if (articleRepository.incrementViews(id) > 0) {
            article.setViews((article.getViews() != null ? article.getViews() : 0) + 1);
        }
        return Optional.of(article);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Article> getPortalArticles(Long categoryId, Long tagId, String keyword, Pageable pageable) {
        String kw = keyword == null ? "" : keyword.trim();
        Page<Article> page;
        if (categoryId == null && tagId == null && kw.isEmpty()) {
            page = articleRepository.findByIsDraftAndIsDeletedOrderByPinnedDescUpdatedAtDescCreatedAtDesc(
                    false, false, pageable);
        } else {
            page = articleRepository.searchPortal(categoryId, tagId, kw, pageable);
        }
        for (Article a : page.getContent()) {
            attachTagsFromJoinTable(a);
        }
        return page;
    }

    @Override
    public Page<Article> getArticlesForAdmin(String keyword, Boolean isDraft, Pageable pageable) {
        String kw = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
        log.info("分页查询文章 (后台管理), 关键字: {}, isDraft: {}", kw, isDraft);
        return articleRepository.findAdminList(isDraft, kw, pageable);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        log.warn("逻辑删除文章, ID: {}", id);
        articleRepository.findById(id).ifPresent(article -> {
            article.setIsDeleted(true);
            articleRepository.save(article);
            // 逻辑删除后清理分块（事务提交后异步执行）
            triggerChunkDelete(id);
        });
    }

    /**
     * 事务提交后异步触发分块向量化。
     * <p>用 {@link TransactionSynchronizationManager} 确保文章已落库再分块，
     * 避免异步任务读到未提交的数据。无事务上下文时直接执行。</p>
     */
    private void triggerChunkUpsert(Article article) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    articleChunkService.upsertChunksAsync(article);
                }
            });
        } else {
            articleChunkService.upsertChunksAsync(article);
        }
    }

    private void triggerChunkDelete(Long articleId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    articleChunkService.deleteChunksAsync(articleId);
                }
            });
        } else {
            articleChunkService.deleteChunksAsync(articleId);
        }
    }

    @Override
    @Transactional
    public void incrementViews(Long id) {
        articleRepository.incrementViews(id);
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

    @Override
    @Transactional
    public Article setArticlePinned(Long id, boolean pinned) {
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在: " + id));
        if (Boolean.TRUE.equals(a.getIsDeleted())) {
            throw new IllegalArgumentException("已删除的文章无法修改置顶");
        }
        a.setPinned(pinned);
        log.info("文章置顶 id={} pinned={}", id, pinned);
        return articleRepository.save(a);
    }
}
