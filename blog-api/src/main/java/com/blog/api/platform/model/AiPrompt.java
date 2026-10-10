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
 * 提示词表 (ai_prompt)
 * <p>System Prompt 模板，按 {@code code + version} 版本化管理。
 * 同一 code 只有一个 {@code is_active=true} 的生效版本，支持回滚。</p>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_prompt",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_prompt_code_version",
                columnNames = {"code", "version"}))
public class AiPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** Prompt 业务编码，如 "agent_system"、"write_section" */
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String code;

    /** 显示名称 */
    @Column(nullable = false, length = 200)
    @ToString.Include
    private String name;

    private String description;

    /** 类型：SYSTEM / TOOL / TEMPLATE */
    @Column(nullable = false, length = 20)
    @ToString.Include
    private String type;

    /** Prompt 正文，含 {placeholders} 占位符 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 占位符定义 JSON，如 [{"name":"topic","desc":"文章主题"}] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String variables;

    /** 版本号，同一 code 自增 */
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    /** 当前生效版本（同一 code 仅一个 active） */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
