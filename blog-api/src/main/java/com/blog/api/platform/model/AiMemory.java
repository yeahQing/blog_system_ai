package com.blog.api.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 会话记忆表 (ai_memory)
 * <p>存储多轮对话上下文。messages 保留精简历史，summary 为长对话压缩摘要。
 * 单会话最多保留最近 20 轮，超出由滑动窗口 + 摘要压缩。</p>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_memory",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_memory_session", columnNames = "session_id"),
        indexes = @Index(name = "idx_ai_memory_user", columnList = "user_id"))
public class AiMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** 会话 ID */
    @Column(nullable = false, unique = true, length = 64)
    @ToString.Include
    private String sessionId;

    /** 关联用户（博主） */
    @Column(name = "user_id")
    @ToString.Include
    private Long userId;

    /** 历史消息数组 JSON（精简版，不含工具细节） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String messages;

    /** 滑动窗口摘要（长对话压缩） */
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "last_active_at", nullable = false)
    @ToString.Include
    private LocalDateTime lastActiveAt;
}
