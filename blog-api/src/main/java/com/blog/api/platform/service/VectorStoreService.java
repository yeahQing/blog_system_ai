package com.blog.api.platform.service;

import com.blog.api.platform.model.AiVectorStore;
import com.blog.api.platform.registry.AiVectorStoreRepository;
import com.blog.api.platform.registry.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 向量库管理服务：多 Collection 路由 + 动态建表。
 * <p>每个 Collection 对应一张物理表（共享 chunk 结构），表名存于 {@code ai_vector_store.table_name}。
 * 物理表结构：id / business_id / chunk_index / chunk_type / content / content_hash / embedding(vector) / created_at。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private static final String VECTOR_TYPE = "vector";

    private final AiVectorStoreRepository storeRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ModelRegistry modelRegistry;

    /**
     * 创建新的 Collection（注册元数据 + 动态建物理表）。
     * <p>物理表名规则：blog_{business_type_lower}_chunk。</p>
     */
    @Transactional
    public AiVectorStore createCollection(String code, String name, String businessType,
                                          Long embeddingModelId, String chunkStrategy,
                                          Integer chunkMaxChars, String description) {
        // 校验 code 唯一
        storeRepository.findByCode(code).ifPresent(s -> {
            throw new IllegalArgumentException("Collection code 已存在: " + code);
        });

        int dimension = modelRegistry.getDefaultEmbeddingDimension();
        String tableName = "blog_" + businessType.toLowerCase() + "_chunk";

        // 物理表可能已被其他 Collection 引用（同一 business_type 仅一个表），不存在则建表
        createChunkTableIfNotExists(tableName, dimension);

        AiVectorStore store = AiVectorStore.builder()
                .code(code)
                .name(name)
                .tableName(tableName)
                .businessType(businessType)
                .embeddingModelId(embeddingModelId)
                .dimension(dimension)
                .chunkStrategy(chunkStrategy)
                .chunkMaxChars(chunkMaxChars == null ? 800 : chunkMaxChars)
                .description(description)
                .enabled(true)
                .build();
        return storeRepository.save(store);
    }

    /** 按业务类型获取启用的 Collection */
    public Optional<AiVectorStore> getByBusiness(String businessType) {
        return storeRepository.findByBusinessTypeAndEnabledTrue(businessType);
    }

    /** 按 code 获取 Collection */
    public Optional<AiVectorStore> getByCode(String code) {
        return storeRepository.findByCode(code);
    }

    /** 列出所有启用的 Collection */
    public List<AiVectorStore> listEnabled() {
        return storeRepository.findByEnabledTrue();
    }

    /** 列出全部 Collection */
    public List<AiVectorStore> listAll() {
        return storeRepository.findAll();
    }

    /** 启用/禁用 Collection（不删物理表） */
    @Transactional
    public AiVectorStore toggleEnabled(Long id, boolean enabled) {
        AiVectorStore s = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection 不存在: id=" + id));
        s.setEnabled(enabled);
        return storeRepository.save(s);
    }

    /**
     * 动态建 chunk 物理表（如不存在）。
     * <p>注意：vector 维度在建表时固定。若后续切换 Embedding 模型导致维度变化，需手动重建表。</p>
     */
    private void createChunkTableIfNotExists(String tableName, int dimension) {
        String vectorType = VECTOR_TYPE + "(" + dimension + ")";
        // 表名做白名单校验，防 SQL 注入（仅允许字母数字下划线）
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
        String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id BIGSERIAL PRIMARY KEY,
                    business_id BIGINT NOT NULL,
                    chunk_index INT NOT NULL,
                    chunk_type VARCHAR(20) NOT NULL,
                    content TEXT NOT NULL,
                    content_hash VARCHAR(64),
                    embedding %s NOT NULL,
                    created_at TIMESTAMP DEFAULT NOW(),
                    CONSTRAINT uk_%s_chunk UNIQUE (business_id, chunk_index)
                )
                """.formatted(tableName, vectorType, tableName);

        String hnswIndex = "CREATE INDEX IF NOT EXISTS idx_%s_hnsw ON %s USING hnsw (embedding vector_cosine_ops)"
                .formatted(tableName, tableName);
        String bizIndex = "CREATE INDEX IF NOT EXISTS idx_%s_business ON %s(business_id)"
                .formatted(tableName, tableName);

        jdbcTemplate.execute(sql);
        jdbcTemplate.execute(hnswIndex);
        jdbcTemplate.execute(bizIndex);
        log.info("向量库物理表已就绪: {} (dimension={})", tableName, dimension);
    }
}
