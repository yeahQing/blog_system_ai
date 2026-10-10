package com.blog.api.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 向量库元数据表 (ai_vector_store)
 * <p>管理多个 Collection，不同业务用不同物理表。
 * 实际物理表（如 blog_article_chunk）由 {@code VectorStoreService} 动态创建。</p>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_vector_store",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_vector_store_code", columnNames = "code"))
public class AiVectorStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** 唯一编码，如 "article_chunks"、"comment_chunks" */
    @Column(nullable = false, unique = true, length = 100)
    @ToString.Include
    private String code;

    /** 显示名称 */
    @Column(nullable = false, length = 200)
    @ToString.Include
    private String name;

    /** 实际物理表名，如 "blog_article_chunk" */
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String tableName;

    /** 业务类型：ARTICLE / COMMENT / GUESTBOOK */
    @Column(nullable = false, length = 50)
    @ToString.Include
    private String businessType;

    /** 关联的 Embedding 模型 ID（ai_model.id） */
    @Column(name = "embedding_model_id")
    private Long embeddingModelId;

    /** 向量维度 */
    @Column(nullable = false)
    @ToString.Include
    private Integer dimension;

    /** 分块策略：MARKDOWN / PLAINTEXT / CODE */
    @Column(length = 20)
    @ToString.Include
    private String chunkStrategy;

    /** 分块最大字符数 */
    @Builder.Default
    private Integer chunkMaxChars = 800;

    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
