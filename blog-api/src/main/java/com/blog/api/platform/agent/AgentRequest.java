package com.blog.api.platform.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体对话请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    /** 用户输入 */
    private String message;
    /** 会话 ID（为空则新建） */
    private String sessionId;
    /** 用户 ID（博主），用于记忆关联 */
    private Long userId;
}
