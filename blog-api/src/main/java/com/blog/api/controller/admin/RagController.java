package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.entity.Article;
import com.blog.api.platform.rag.ArticleChunkService;
import com.blog.api.platform.rag.ChunkSearchHit;
import com.blog.api.platform.rag.RagService;
import com.blog.api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG 检索接口（AI 控制台）。
 * <p>提供历史文章向量回填、语义检索调试。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/platform/rag")
@RequiredArgsConstructor
public class RagController {

    private final ArticleChunkService articleChunkService;
    private final RagService ragService;
    private final ArticleRepository articleRepository;

    /**
     * 历史文章向量批量回填。
     * <p>遍历所有已发布未删除文章，分块并向量化。已处理且内容未变的文章自动跳过。</p>
     */
    @PostMapping("/backfill")
    public ResponseEntity<Result<Map<String, Integer>>> backfill() {
        List<Article> articles = articleRepository.findByIsDraftFalseAndIsDeletedFalse();
        int total = articles.size();
        int processed = 0;
        int skipped = 0;
        for (Article a : articles) {
            int n = articleChunkService.upsertChunks(a);
            if (n > 0) {
                processed++;
            } else {
                skipped++;
            }
        }
        log.info("向量回填完成: 总计={}, 新建={}, 跳过={}", total, processed, skipped);
        return ResponseEntity.ok(Result.success(Map.of(
                "total", total,
                "processed", processed,
                "skipped", skipped)));
    }

    /**
     * 语义检索测试：输入关键词，返回 Top-K 相关知识块。
     */
    @GetMapping("/search")
    public ResponseEntity<Result<List<ChunkSearchHit>>> search(@RequestParam String query,
                                                               @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(Result.success(ragService.retrieve(query, limit)));
    }

    /**
     * 查看检索 + 拼装后的上下文（供智能体 Prompt 使用）。
     */
    @GetMapping("/context")
    public ResponseEntity<Result<Map<String, Object>>> context(@RequestParam String query,
                                                               @RequestParam(required = false) Integer limit) {
        List<ChunkSearchHit> hits = ragService.retrieve(query, limit);
        String context = ragService.buildContext(hits);
        return ResponseEntity.ok(Result.success(Map.of(
                "hits", hits,
                "context", context,
                "hitCount", hits.size())));
    }
}
