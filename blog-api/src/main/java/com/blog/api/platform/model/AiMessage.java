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
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 消息表 (ai_message)
 * <p>记录智能体每一次对话的完整消息流，是可观测性的核心。
 * 按 session_id / agent_run_id 查询轨迹，role 区分 SYSTEM/USER/ASSISTANT/TOOL。</p>
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_message",
        indexes = {
                @Index(name = "idx_ai_message_session", columnList = "session_id, created_at"),
                @Index(name = "idx_ai_message_run", columnList = "agent_run_id"),
                @Index(name = "idx_ai_message_role", columnList = "role")
        })
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** 会话 ID */
    @Column(nullable = false, length = 64)
    @ToString.Include
    private String sessionId;

    /** 单次执行 ID（一次用户输入 → 多步工具调用） */
    @Column(length = 64)
    @ToString.Include
    private String agentRunId;

    /** 角色：SYSTEM / USER / ASSISTANT / TOOL */
    @Column(nullable = false, length = 20)
    @ToString.Include
    private String role;

    /** 消息内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** TOOL 角色时：调用的工具名 */
    @Column(length = 100)
    private String toolName;

    /** 工具调用 ID（关联请求与响应） */
    @Column(length = 64)
    private String toolCallId;

    /** 工具入参 JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String toolArgs;

    /** 工具返回结果 JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String toolResult;

    /** 生成该消息的模型 */
    @Column(length = 100)
    private String modelName;

    /** 输入 token */
    private Integer tokenInput;

    /** 输出 token */
    private Integer tokenOutput;

    /** 耗时（毫秒） */
    private Integer latencyMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
