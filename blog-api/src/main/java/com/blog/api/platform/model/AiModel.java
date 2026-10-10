package com.blog.api.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * AI 模型配置表 (ai_model)
 * <p>统一管理 CHAT / EMBEDDING 两类模型，运行时由 {@code ModelRegistry} 动态构建 Bean。
 * API Key 加密存储，前端仅展示掩码。</p>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_model",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_model_name", columnNames = "name"))
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** 唯一名称，如 "minimax-m2.7"、"ollama-bge-m3" */
    @Column(nullable = false, unique = true, length = 100)
    @ToString.Include
    private String name;

    /** 模型类型：CHAT / EMBEDDING */
    @Column(nullable = false, length = 20)
    @ToString.Include
    private String type;

    /** 提供方：MINIMAX / OLLAMA / OPENAI_COMPATIBLE */
    @Column(nullable = false, length = 30)
    @ToString.Include
    private String provider;

    /** 模型 ID，如 "MiniMax-M2.7"、"bge-m3" */
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String modelId;

    /** 基础地址，如 "https://api.minimax.chat"、"http://blog-ollama:11434" */
    @Column(length = 255)
    private String baseUrl;

    /** 加密存储的 API Key（明文不落库） */
    @Column(length = 500)
    private String apiKeyEncrypted;

    /** Embedding 维度（仅 EMBEDDING 类型） */
    private Integer dimension;

    /** 通用参数：temperature / maxTokens / topK 等 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String params;

    /** 是否该 type 的默认模型 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
