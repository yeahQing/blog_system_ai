package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.ArticleSaveRequest;
import com.blog.api.entity.Article;
import com.blog.api.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

@Slf4j
@Tag(name = "文章管理模块", description = "提供文章的增删改查 API")
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "保存文章 (新增/修改)")
    @PostMapping("/save")
    public ResponseEntity<Result<Article>> saveArticle(@RequestBody ArticleSaveRequest request) {
        log.info("接收到保存文章请求: {}", request.getTitle());
        Article saved = articleService.saveArticle(request);
        return ResponseEntity.ok(Result.success(saved));
    }

    @Operation(summary = "获取文章详情 (前台公开，仅已发布)")
    @GetMapping("/portal/{id:\\d+}")
    public ResponseEntity<Result<Article>> getPortalArticle(@PathVariable Long id) {
        log.info("前台获取文章详情, ID: {}", id);
        return articleService.getArticleForPortal(id)
                .map(a -> ResponseEntity.ok(Result.success(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "相关文章推荐")
    @GetMapping("/portal/related/{id:\\d+}")
    public ResponseEntity<Result<List<Article>>> relatedArticles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(Result.success(articleService.getRelatedArticles(id, limit)));
    }

    @Operation(summary = "文章点赞")
    @PostMapping("/portal/{id:\\d+}/like")
    public ResponseEntity<Result<Void>> likeArticle(@PathVariable Long id) {
        if (!articleService.likeArticle(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Result.success());
    }

    @Operation(summary = "获取文章详情 (管理端，需登录)")
    @GetMapping("/admin/{id:\\d+}")
    public ResponseEntity<Result<Article>> getAdminArticle(@PathVariable Long id) {
        log.info("管理端获取文章详情, ID: {}", id);
        return articleService.getArticleForAdminById(id)
                .map(a -> ResponseEntity.ok(Result.success(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "分页获取文章列表 (前台展示，支持分类/标签/搜索)")
    @GetMapping("/portal/list")
    public ResponseEntity<Result<Page<Article>>> getPortalList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("前台分页获取文章列表 categoryId={} tagId={} keyword={}", categoryId, tagId, keyword);
        return ResponseEntity.ok(Result.success(articleService.getPortalArticles(categoryId, tagId, keyword, pageable)));
    }

    @Operation(summary = "分页获取文章列表 (管理后台)")
    @GetMapping("/admin/list")
    public ResponseEntity<Result<Page<Article>>> getAdminList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isDraft,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("管理后台分页获取文章列表 isDraft={}", isDraft);
        return ResponseEntity.ok(Result.success(articleService.getArticlesForAdmin(keyword, isDraft, pageable)));
    }

    @Operation(summary = "发布文章 (草稿 → 已发布，管理后台)")
    @PostMapping("/admin/{id:\\d+}/publish")
    public ResponseEntity<Result<Article>> publishArticle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Result.success(articleService.publishArticle(id)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "下架文章 (已发布 → 草稿，前台不可见，管理后台)")
    @PostMapping("/admin/{id:\\d+}/unpublish")
    public ResponseEntity<Result<Article>> unpublishArticle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Result.success(articleService.unpublishArticle(id)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "置顶 / 取消置顶 (管理后台)")
    @PostMapping("/admin/{id:\\d+}/pinned")
    public ResponseEntity<Result<Article>> setArticlePinned(
            @PathVariable Long id,
            @RequestParam boolean pinned) {
        try {
            return ResponseEntity.ok(Result.success(articleService.setArticlePinned(id, pinned)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "逻辑删除文章 (管理后台)")
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Result<Void>> deleteArticle(@PathVariable Long id) {
        log.warn("管理后台请求删除文章, ID: {}", id);
        articleService.deleteArticle(id);
        return ResponseEntity.ok(Result.success());
    }
}
