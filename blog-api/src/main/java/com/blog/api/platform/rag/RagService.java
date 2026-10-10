package com.blog.api.platform.rag;

import com.blog.api.platform.model.AiVectorStore;
import com.blog.api.platform.registry.AiVectorStoreRepository;
import com.blog.api.platform.registry.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索增强生成核心服务：混合检索 + 重排序 + 上下文拼装。
 * <p>① 查询向量化（Ollama）→ ② 混合检索（pgvector 语义 + ILIKE 关键词加权融合）
 * → ③ 阈值过滤 + 同文章去重 → ④ 可选重排序 → ⑤ 拼装截断上下文。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final String DEFAULT_COLLECTION_CODE = "article_chunks";

    /** 语义检索权重 */
    private static final double SEMANTIC_WEIGHT = 0.7;
    /** 关键词检索权重 */
    private static final double KEYWORD_WEIGHT = 0.3;

    private final ModelRegistry modelRegistry;
    private final AiVectorStoreRepository vectorStoreRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RerankService rerankService;

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.min-score:0.3}")
    private double minScore;

    @Value("${rag.max-context-chars:4000}")
    private int maxContextChars;

    /** 候选池大小（重排序前多取，默认 20） */
    @Value("${rag.candidate-pool:20}")
    private int candidatePool;

    /** 是否启用重排序 */
    @Value("${rag.enable-rerank:false}")
    private boolean enableRerank;

    /**
     * 混合检索：语义 + 关键词加权融合。
     *
     * @param query 用户查询文本
     * @param limit 返回条数（null 用默认 topK）
     */
    public List<ChunkSearchHit> retrieve(String query, Integer limit) {
        AiVectorStore store = getCollection();
        int k = limit != null ? limit : topK;

        // ① 查询向量化
        float[] queryVec = modelRegistry.embedWithDefault(query);
        String vecLiteral = toVectorLiteral(queryVec);

        // ② 混合检索：语义 Top-N + 关键词 Top-N，各自带原始分数
        int poolSize = Math.max(candidatePool, k * 4);
        Map<Long, ChunkSearchHit> semanticHits = semanticSearch(store.getTableName(), vecLiteral, queryVec, poolSize);
        Map<Long, ChunkSearchHit> keywordHits = keywordSearch(store.getTableName(), query, poolSize);

        // ③ 加权融合（同 chunkId 合并分数）
        Map<Long, ChunkSearchHit> merged = mergeScores(semanticHits, keywordHits);

        // ④ 阈值过滤 + 同文章去重（每篇文章只保留最高分块）
        List<ChunkSearchHit> filtered = dedupByArticle(merged, k);

        // ⑤ 可选重排序
        if (enableRerank && filtered.size() > k) {
            filtered = rerank(query, filtered, k);
        }

        return filtered.stream().limit(k).toList();
    }

    /** 语义检索：pgvector 余弦相似度，返回 chunkId → hit */
    private Map<Long, ChunkSearchHit> semanticSearch(String tableName, String vecLiteral, float[] queryVec, int limit) {
        String sql = """
                SELECT c.id, c.business_id, c.chunk_index, c.chunk_type, c.content,
                       1 - (c.embedding <=> ?::vector) AS score,
                       a.title AS article_title
                FROM %s c
                JOIN blog_article a ON a.id = c.business_id
                WHERE a.is_draft = false AND a.is_deleted = false
                ORDER BY c.embedding <=> ?::vector
                LIMIT ?
                """.formatted(tableName);
        try {
            List<ChunkSearchHit> hits = jdbcTemplate.query(sql,
                    (rs, i) -> new ChunkSearchHit(
                            rs.getLong("id"),
                            rs.getLong("business_id"),
                            rs.getInt("chunk_index"),
                            rs.getString("chunk_type"),
                            rs.getString("content"),
                            rs.getDouble("score"),
                            rs.getString("article_title")),
                    vecLiteral, vecLiteral, limit);
            return hits.stream().collect(Collectors.toMap(ChunkSearchHit::getChunkId, h -> h, (a, b) -> a, LinkedHashMap::new));
        } catch (Exception e) {
            log.warn("语义检索失败，仅用关键词检索: {}", e.getMessage());
            return Map.of();
        }
    }

    /** 关键词检索：ILIKE 模糊匹配，分数基于命中关键词数归一化 */
    private Map<Long, ChunkSearchHit> keywordSearch(String tableName, String query, int limit) {
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return Map.of();
        }
        // 用 OR 拼接 ILIKE 条件
        String orClause = keywords.stream()
                .map(k -> "c.content ILIKE ?")
                .collect(Collectors.joining(" OR "));
        String sql = """
                SELECT c.id, c.business_id, c.chunk_index, c.chunk_type, c.content, a.title AS article_title
                FROM %s c
                JOIN blog_article a ON a.id = c.business_id
                WHERE a.is_draft = false AND a.is_deleted = false
                AND (%s)
                LIMIT ?
                """.formatted(tableName, orClause);
        Object[] params = new Object[keywords.size() + 1];
        for (int i = 0; i < keywords.size(); i++) {
            params[i] = "%" + keywords.get(i) + "%";
        }
        params[keywords.size()] = limit;
        try {
            List<ChunkSearchHit> hits = jdbcTemplate.query(sql,
                    (rs, i) -> {
                        String content = rs.getString("content");
                        // 关键词命中数归一化到 0~1
                        double hitCount = keywords.stream().filter(k -> content.toLowerCase().contains(k.toLowerCase())).count();
                        double score = hitCount / keywords.size();
                        return new ChunkSearchHit(
                                rs.getLong("id"),
                                rs.getLong("business_id"),
                                rs.getInt("chunk_index"),
                                rs.getString("chunk_type"),
                                content,
                                score,
                                rs.getString("article_title"));
                    },
                    params);
            return hits.stream().collect(Collectors.toMap(ChunkSearchHit::getChunkId, h -> h, (a, b) -> a, LinkedHashMap::new));
        } catch (Exception e) {
            log.warn("关键词检索失败，仅用语义检索: {}", e.getMessage());
            return Map.of();
        }
    }

    /** 加权融合：语义 0.7 + 关键词 0.3，仅出现在一方的按其权重计 */
    private Map<Long, ChunkSearchHit> mergeScores(Map<Long, ChunkSearchHit> semantic, Map<Long, ChunkSearchHit> keyword) {
        Map<Long, ChunkSearchHit> merged = new LinkedHashMap<>();
        Set<Long> allIds = new LinkedHashSet<>();
        allIds.addAll(semantic.keySet());
        allIds.addAll(keyword.keySet());
        for (Long id : allIds) {
            ChunkSearchHit s = semantic.get(id);
            ChunkSearchHit k = keyword.get(id);
            ChunkSearchHit base = s != null ? s : k;
            double semScore = s != null ? s.getScore() : 0;
            double kwScore = k != null ? k.getScore() : 0;
            // 仅有一方时，该方权重提升（避免被压低）
            double fused;
            if (s != null && k != null) {
                fused = SEMANTIC_WEIGHT * semScore + KEYWORD_WEIGHT * kwScore;
            } else if (s != null) {
                fused = semScore;
            } else {
                fused = kwScore;
            }
            base.setScore(fused);
            merged.put(id, base);
        }
        return merged;
    }

    /** 同文章去重 + 阈值过滤 + 降序排序 */
    private List<ChunkSearchHit> dedupByArticle(Map<Long, ChunkSearchHit> merged, int limit) {
        Map<Long, ChunkSearchHit> deduped = new LinkedHashMap<>();
        for (ChunkSearchHit h : merged.values()) {
            if (h.getScore() < minScore) {
                continue;
            }
            deduped.merge(h.getArticleId(), h, (a, b) -> a.getScore() >= b.getScore() ? a : b);
        }
        return deduped.values().stream()
                .sorted(Comparator.comparingDouble(ChunkSearchHit::getScore).reversed())
                .toList();
    }

    /** 重排序：用 MiniMax 对候选块与查询的相关性打分 */
    private List<ChunkSearchHit> rerank(String query, List<ChunkSearchHit> candidates, int topN) {
        log.debug("重排序 {} 个候选", candidates.size());
        return rerankService.rerank(query, candidates, topN);
    }

    /**
     * 拼装上下文：去重 → 排序 → 截断到 maxContextChars。
     */
    public String buildContext(List<ChunkSearchHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "（未检索到相关历史内容）";
        }
        StringBuilder sb = new StringBuilder();
        for (ChunkSearchHit h : hits) {
            String block = "【文章：" + h.getArticleTitle() + "（相关度:" + String.format("%.2f", h.getScore()) + "）】\n"
                    + h.getContent();
            if (sb.length() + block.length() + 5 > maxContextChars) {
                sb.append("\n…（更多相关内容已省略）");
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n\n---\n\n");
            }
            sb.append(block);
        }
        return sb.toString();
    }

    /** 从查询中提取关键词（按空格/标点切分，去停用词，长度≥2） */
    static List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> stopWords = Set.of("的", "了", "和", "是", "在", "我", "有", "与", "及", "或", "a", "an", "the", "of", "to", "in", "for", "and");
        return Arrays.stream(query.split("[\\s,，。.!！?？、:：;；]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .filter(s -> !stopWords.contains(s.toLowerCase()))
                .distinct()
                .toList();
    }

    /** 获取默认 ARTICLE Collection */
    private AiVectorStore getCollection() {
        return vectorStoreRepository.findByBusinessTypeAndEnabledTrue("ARTICLE")
                .or(() -> vectorStoreRepository.findByCode(DEFAULT_COLLECTION_CODE))
                .orElseThrow(() -> new IllegalStateException(
                        "未找到 ARTICLE 向量库 Collection，请先创建文章并完成分块"));
    }

    /** float[] → pgvector 字面量 [0.1,0.2,...] */
    static String toVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        return sb.append("]").toString();
    }
}
