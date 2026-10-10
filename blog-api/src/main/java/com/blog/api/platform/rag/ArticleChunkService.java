package com.blog.api.platform.rag;

import com.blog.api.entity.Article;
import com.blog.api.platform.model.AiVectorStore;
import com.blog.api.platform.registry.AiVectorStoreRepository;
import com.blog.api.platform.registry.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 文章分块生命周期管理：upsert / delete / backfill。
 * <p>分块写入物理表（表名由 {@link AiVectorStore#getTableName()} 指定，默认 blog_article_chunk）。
 * 内容哈希判断是否需要重建，避免重复向量化。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleChunkService {

    private static final String DEFAULT_BUSINESS = "ARTICLE";
    private static final String DEFAULT_COLLECTION_CODE = "article_chunks";

    private final MarkdownChunkSplitter splitter;
    private final ModelRegistry modelRegistry;
    private final AiVectorStoreRepository vectorStoreRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 文章保存/更新后调用：内容变化才重建分块（异步，不阻塞主流程）。
     */
    @Async
    public void upsertChunksAsync(Article article) {
        try {
            upsertChunks(article);
        } catch (Exception e) {
            log.error("文章分块向量化失败 articleId={}: {}", article.getId(), e.getMessage(), e);
        }
    }

    /** 同步重建分块（用于回填接口） */
    public int upsertChunks(Article article) {
        AiVectorStore store = requireCollection();
        String tableName = store.getTableName();

        String contentHash = sha256(article.getTitle() + "\n" + nullSafe(article.getSummary()) + "\n" + nullSafe(article.getContent()));

        // 内容未变化则跳过
        if (isContentUnchanged(tableName, article.getId(), contentHash)) {
            log.debug("文章内容未变化，跳过分块 articleId={}", article.getId());
            return 0;
        }

        // 删旧块
        deleteByArticleId(tableName, article.getId());

        // 分块：TITLE + SUMMARY + 正文 chunks
        List<Chunk> chunks = new ArrayList<>();
        int idx = 0;
        if (article.getTitle() != null && !article.getTitle().isBlank()) {
            chunks.add(new Chunk(idx++, "TITLE", article.getTitle()));
        }
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            chunks.add(new Chunk(idx++, "SUMMARY", article.getSummary()));
        }
        chunks.addAll(splitter.split(article.getContent()));
        // 重排 index
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setIndex(i);
        }

        if (chunks.isEmpty()) {
            log.info("文章无有效内容可分块 articleId={}", article.getId());
            return 0;
        }

        // 批量向量化
        List<String> texts = chunks.stream().map(Chunk::getContent).toList();
        List<float[]> embeddings = modelRegistry.embedBatchWithDefault(texts);

        // 批量插入
        int inserted = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            insertChunk(tableName, article.getId(), c.getIndex(), c.getType(), c.getContent(), contentHash, embeddings.get(i));
            inserted++;
        }
        log.info("文章分块完成 articleId={}, 块数={}", article.getId(), inserted);
        return inserted;
    }

    /** 文章删除时清理分块（级联删除由 FK 保证，这里兜底） */
    @Async
    public void deleteChunksAsync(Long articleId) {
        try {
            AiVectorStore store = requireCollection();
            deleteByArticleId(store.getTableName(), articleId);
            log.info("已删除文章分块 articleId={}", articleId);
        } catch (Exception e) {
            log.warn("删除文章分块失败 articleId={}: {}", articleId, e.getMessage());
        }
    }

    /** 获取或创建默认 ARTICLE Collection（首次调用时若不存在则创建元数据，物理表需预先存在） */
    private AiVectorStore requireCollection() {
        Optional<AiVectorStore> opt = vectorStoreRepository.findByBusinessTypeAndEnabledTrue(DEFAULT_BUSINESS);
        if (opt.isPresent()) {
            return opt.get();
        }
        // 兜底：按 code 查
        return vectorStoreRepository.findByCode(DEFAULT_COLLECTION_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "未找到 ARTICLE 向量库 Collection，请先在「AI 控制台-向量库」创建 article_chunks"));
    }

    private boolean isContentUnchanged(String tableName, Long articleId, String newHash) {
        try {
            String sql = "SELECT content_hash FROM " + tableName + " WHERE business_id = ? ORDER BY chunk_index LIMIT 1";
            List<String> hashes = jdbcTemplate.queryForList(sql, String.class, articleId);
            if (hashes.isEmpty()) {
                return false;
            }
            return newHash.equals(hashes.get(0));
        } catch (Exception e) {
            log.warn("查询内容哈希失败，按未变化处理: {}", e.getMessage());
            return false;
        }
    }

    private void deleteByArticleId(String tableName, Long articleId) {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE business_id = ?", articleId);
    }

    private void insertChunk(String tableName, Long articleId, int chunkIndex, String chunkType,
                             String content, String contentHash, float[] embedding) {
        // 构造 pgvector 字面量：[0.1,0.2,...]
        StringBuilder vec = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) vec.append(",");
            vec.append(embedding[i]);
        }
        vec.append("]");
        String sql = "INSERT INTO " + tableName
                + " (business_id, chunk_index, chunk_type, content, content_hash, embedding) VALUES (?, ?, ?, ?, ?, ?::vector)";
        jdbcTemplate.update(sql, articleId, chunkIndex, chunkType, content, contentHash, vec.toString());
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
